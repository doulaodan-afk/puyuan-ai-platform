import { useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { accountService } from '@/services/account';
import './index.scss';

export default function AccountPage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();

  useEffect(() => {
    checkAuth();
    loadData();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/account/index')
      });
      return;
    }
  };

  const loadData = async () => {
    try {
      await accountStore.fetchBalance();
    } catch (error) {
      console.error('加载失败:', error);
    }
  };

  const handleRefresh = () => {
    loadData();
  };

  const navigateTo = (url: string) => {
    Taro.navigateTo({ url });
  };

  const handleLogout = () => {
    Taro.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          authStore.clearAuth();
          Taro.reLaunch({ url: '/pages/login/index' });
        }
      }
    });
  };

  return (
    <ScrollView
      className="page"
      scrollY
      refresherEnabled
      onRefresherRefresh={handleRefresh}
    >
      <View className="header">
        <View className="avatar">{authStore.nickname?.charAt(0) || 'U'}</View>
        <View className="user-info">
          <Text className="name">{authStore.nickname || '用户'}</Text>
          <Text className="mobile">{authStore.mobile}</Text>
        </View>
      </View>

      <View className="balance-section">
        <Text className="balance-label">账户余额</Text>
        <Text className="balance-amount">{accountStore.balanceText}</Text>
        <Text className="balance-unit">Tokens</Text>
        <View className="balance-actions">
          <View className="action-btn primary" onClick={() => navigateTo('/pages/recharge/index')}>
            立即充值
          </View>
          <View className="action-btn" onClick={() => navigateTo('/pages/ledger/index')}>
            消费明细
          </View>
        </View>
      </View>

      <View className="storage-section">
        <View className="storage-title">存储空间</View>
        <View className="storage-info">
          <View className="storage-item">
            <Text className="storage-label">已使用</Text>
            <Text className="storage-value">{accountStore.storageUsedText}</Text>
          </View>
          <View className="storage-item">
            <Text className="storage-label">总配额</Text>
            <Text className="storage-value">{accountStore.storageQuotaText}</Text>
          </View>
          <View className="storage-item">
            <Text className="storage-label">到期时间</Text>
            <Text className="storage-value">{accountStore.balance?.expireDate || '永久'}</Text>
          </View>
        </View>
      </View>

      <View className="menu-section">
        <View className="menu-item" onClick={() => navigateTo('/pages/billing/index')}>
          <View className="menu-icon">📄</View>
          <Text className="menu-text">账单中心</Text>
          <Text className="menu-arrow">›</Text>
        </View>
        <View className="menu-item" onClick={() => navigateTo('/pages/ledger/index')}>
          <View className="menu-icon">📊</View>
          <Text className="menu-text">消费记录</Text>
          <Text className="menu-arrow">›</Text>
        </View>
        <View className="menu-item" onClick={() => navigateTo('/pages/recharge/index')}>
          <View className="menu-icon">💰</View>
          <Text className="menu-text">充值记录</Text>
          <Text className="menu-arrow">›</Text>
        </View>
      </View>

      <View className="logout-section">
        <View className="logout-btn" onClick={handleLogout}>
          退出登录
        </View>
      </View>
    </ScrollView>
  );
}
