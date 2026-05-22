import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Button } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { accountService, RechargePackage, WxPrepayResponse } from '@/services/account';
import { subscribeService } from '@/services/subscribe';
import './index.scss';

export default function RechargePage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [packages, setPackages] = useState<RechargePackage[]>([]);
  const [selectedPackage, setSelectedPackage] = useState<RechargePackage | null>(null);
  const [loading, setLoading] = useState(false);
  const [paying, setPaying] = useState(false);
  const [orderNo, setOrderNo] = useState('');
  const [mockMode, setMockMode] = useState(true); // 默认使用 Mock 模式

  useEffect(() => {
    checkAuth();
    loadPackages();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/recharge/index')
      });
      return;
    }
  };

  const loadPackages = async () => {
    try {
      const pkgs = await accountService.getPackages();
      setPackages(pkgs);
    } catch (error) {
      console.error('加载套餐失败:', error);
    }
  };

  const handleSelectPackage = (pkg: RechargePackage) => {
    setSelectedPackage(pkg);
  };

  const handleRecharge = async () => {
    if (!selectedPackage) {
      Taro.showToast({
        title: '请选择充值套餐',
        icon: 'none'
      });
      return;
    }

    // Mock 模式下直接调用支付成功接口
    if (mockMode) {
      await mockRecharge();
      return;
    }

    // 真实支付模式
    await realRecharge();
  };

  // Mock 充值流程（开发环境）
  const mockRecharge = async () => {
    setPaying(true);
    try {
      // 1. 创建订单（先调用原接口）
      const order = await accountService.createOrder({
        packageId: selectedPackage!.id,
        amount: selectedPackage!.amount,
        tokenGrant: selectedPackage!.tokenGrant
      });

      setOrderNo(order.orderNo);

      // 2. 调用 Mock 支付成功接口
      await accountService.mockPaymentSuccess(order.orderNo);

      // 3. 刷新余额
      await accountStore.fetchBalance();

      // 4. 发送充值成功订阅消息
      try {
        await subscribeService.sendRechargeSuccessNotification({
          tokenGrant: selectedPackage!.tokenGrant,
          orderNo: order.orderNo
        });
      } catch (error) {
        console.error('发送订阅消息失败:', error);
        // 不影响充值成功流程
      }

      Taro.showModal({
        title: '充值成功',
        content: `充值 ${selectedPackage!.amount} 元，获得 ${selectedPackage!.tokenGrant} Tokens`,
        showCancel: false,
        success: () => {
          // 返回上一页
          setTimeout(() => {
            Taro.navigateBack();
          }, 500);
        }
      });
    } catch (error) {
      console.error('充值失败:', error);
      Taro.showToast({
        title: '充值失败，请重试',
        icon: 'none'
      });
    } finally {
      setPaying(false);
      setOrderNo('');
    }
  };

  // 真实支付流程（生产环境）
  const realRecharge = async () => {
    setPaying(true);
    try {
      // 1. 调用微信预下单接口
      const prepayResponse = await accountService.wxPrepay({
        amount: selectedPackage!.amount,
        packageName: selectedPackage!.name
      });

      // 2. 调起微信支付
      await Taro.requestPayment({
        ...prepayResponse,
        success: (res) => {
          console.log('微信支付成功:', res);
          Taro.showToast({
            title: '支付成功',
            icon: 'success'
          });
          // 跳转到订单列表
          setTimeout(() => {
            Taro.redirectTo({ url: '/pages/recharge/orders' });
          }, 1500);
        },
        fail: (err) => {
          console.error('微信支付失败:', err);
          Taro.showToast({
            title: '支付失败，请重试',
            icon: 'none'
          });
        }
      });
    } catch (error) {
      console.error('创建支付订单失败:', error);
      const message = error instanceof Error ? error.message : '创建支付订单失败';
      Taro.showToast({
        title: message,
        icon: 'none'
      });
    } finally {
      setPaying(false);
    }
  };

  return (
    <View className="page">
      <View className="balance-tip">
        <Text className="tip-text">💡 充值后 Token 将立即到账</Text>
      </View>

      <View className="packages-section">
        <Text className="section-title">选择充值套餐</Text>
        <View className="packages-grid">
          {packages.map((pkg) => (
            <View
              key={pkg.id}
              className={`package-card ${selectedPackage?.id === pkg.id ? 'active' : ''}`}
              onClick={() => handleSelectPackage(pkg)}
            >
              {pkg.recommended && (
                <View className="recommended-tag">推荐</View>
              )}
              <Text className="package-name">{pkg.name}</Text>
              <View className="package-amount">
                <Text className="currency">¥</Text>
                <Text className="price">{pkg.amount}</Text>
              </View>
              <View className="package-tokens">
                <Text className="token-count">{pkg.tokenGrant}</Text>
                <Text className="token-unit">Tokens</Text>
                {pkg.bonus && (
                  <Text className="bonus">+{pkg.bonus}赠送</Text>
                )}
              </View>
            </View>
          ))}
        </View>
      </View>

      <View className="tips-section">
        <Text className="tips-title">充值说明</Text>
        <Text className="tips-item">• 充值金额实时到账</Text>
        <Text className="tips-item">• Token 有效期：永久有效</Text>
        <Text className="tips-item">• 如遇充值问题，请联系客服</Text>
      </View>

      {mockMode && (
        <View className="mock-notice">
          <Text className="mock-text">⚠️ 当前为 Mock 模式，不会实际扣款</Text>
        </View>
      )}

      <View className="footer">
        <Button
          className={`recharge-btn ${paying ? 'paying' : ''}`}
          loading={loading || paying}
          disabled={loading || paying || !selectedPackage}
          onClick={handleRecharge}
        >
          {paying ? '处理中...' : selectedPackage ? `立即充值 ¥${selectedPackage.amount}` : '请选择套餐'}
        </Button>
      </View>
    </View>
  );
}
