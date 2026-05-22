import { useState } from 'react';
import Taro, { useRouter } from '@tarojs/taro';
import { View, Button, Text } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { authService } from '@/services/auth';
import './index.scss';

export default function LoginPage() {
  const router = useRouter();
  const authStore = useAuthStore();
  const [loading, setLoading] = useState(false);

  // 微信授权登录
  const handleWxLogin = async () => {
    setLoading(true);
    try {
      // 获取微信登录 code
      const loginRes = await Taro.login();

      if (!loginRes.code) {
        throw new Error('获取微信登录凭证失败');
      }

      // 调用后端接口
      const data = await authService.wxLogin({
        code: loginRes.code
      });

      // 保存认证信息
      authStore.setAuthData(data);

      Taro.showToast({
        title: '登录成功',
        icon: 'success'
      });

      // 跳转到原页面或工作台
      setTimeout(() => {
        const redirect = router.params.redirect;
        if (redirect) {
          Taro.redirectTo({ url: decodeURIComponent(redirect) });
        } else {
          Taro.switchTab({ url: '/pages/index/index' });
        }
      }, 1500);
    } catch (error) {
      console.error('微信登录失败:', error);
      const message = error instanceof Error ? error.message : '登录失败，请重试';
      Taro.showToast({
        title: message,
        icon: 'none',
        duration: 2000
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className="login-page">
      <View className="login-container">
        <View className="logo-section">
          <View className="logo">🧵</View>
          <Text className="title">濮院毛衫 AI 平台</Text>
          <Text className="subtitle">智能 · 高效 · 创新</Text>
        </View>

        <View className="form-section">
          <Button
            className="wx-login-btn"
            loading={loading}
            disabled={loading}
            openType="getUserInfo"
            onGetUserInfo={handleWxLogin}
          >
            <View className="wx-login-content">
              <View className="wx-icon">💚</View>
              <Text className="wx-login-text">
                {loading ? '登录中...' : '微信授权登录'}
              </Text>
            </View>
          </Button>

          <View className="agreement-section">
            <Text className="agreement-text">
              登录即表示同意
              <Text className="link" onClick={() => Taro.navigateTo({ url: '/pages/agreement/user' })}>
                《用户协议》
              </Text>
              和
              <Text className="link" onClick={() => Taro.navigateTo({ url: '/pages/agreement/privacy' })}>
                《隐私政策》
              </Text>
            </Text>
          </View>
        </View>

        <View className="tips-section">
          <Text className="tips">首次登录将自动创建账号</Text>
        </View>
      </View>
    </View>
  );
}
