import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { pluginService, AI_SERVICES } from '@/services/plugin';
import './index.scss';

interface PluginItem {
  pluginId: string;
  pluginName: string;
  pluginType: string;
  description: string;
  iconUrl: string;
  status: number;
  enabled: number;
}

export default function IndexPage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [plugins, setPlugins] = useState<PluginItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuth();
    loadData();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index'
      });
      return;
    }
  };

  const loadData = async () => {
    try {
      setLoading(true);
      await accountStore.fetchBalance();
      const enabledPlugins = await pluginService.getEnabledPlugins();
      setPlugins(enabledPlugins);
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadData();
  };

  const navigateToPlugin = (pluginId: string) => {
    const routeMap: Record<string, string> = {
      [AI_SERVICES.IMAGE_GEN]: '/pages/ai-image/index',
      [AI_SERVICES.SCRIPT_GEN]: '/pages/ai-script/index',
      [AI_SERVICES.TRANSLATE]: '/pages/ai-translate/index'
    };

    const route = routeMap[pluginId];
    if (route) {
      Taro.navigateTo({ url: route });
    } else {
      Taro.showToast({
        title: '该功能正在开发中',
        icon: 'none'
      });
    }
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

  if (loading) {
    return (
      <View className="page loading">
        <Text>加载中...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      className="page"
      scrollY
      refresherEnabled
      onRefresherRefresh={handleRefresh}
    >
      <View className="header">
        <View className="user-info">
          <View className="avatar">{authStore.nickname?.charAt(0) || 'U'}</View>
          <View className="info">
            <Text className="name">{authStore.nickname || '用户'}</Text>
            <Text className="role">{authStore.currentRole || '成员'}</Text>
          </View>
        </View>
        <View className="actions">
          <View className="logout-btn" onClick={handleLogout}>退出</View>
        </View>
      </View>

      <View className="balance-card">
        <Text className="balance-label">账户余额</Text>
        <Text className="balance-amount">{accountStore.balanceText}</Text>
        <Text className="balance-unit">Tokens</Text>
        <View className="balance-info">
          <View className="info-item">
            <Text className="label">存储使用</Text>
            <Text className="value">{accountStore.storageUsedText}</Text>
          </View>
          <View className="divider" />
          <View className="info-item">
            <Text className="label">存储配额</Text>
            <Text className="value">{accountStore.storageQuotaText}</Text>
          </View>
        </View>
        <View className="balance-actions">
          <View className="action-btn primary" onClick={() => navigateTo('/pages/recharge/index')}>
            立即充值
          </View>
          <View className="action-btn" onClick={() => navigateTo('/pages/ledger/index')}>
            消费明细
          </View>
        </View>
      </View>

      <View className="section">
        <View className="section-header">
          <Text className="section-title">AI 工具</Text>
          <View className="section-more" onClick={() => Taro.switchTab({ url: '/pages/plugins/index' })}>
            全部 <Text className="arrow">›</Text>
          </View>
        </View>
        <View className="plugin-grid">
          {plugins.map((plugin) => (
            <View
              key={plugin.pluginId}
              className="plugin-card"
              onClick={() => navigateToPlugin(plugin.pluginId)}
            >
              <View className="plugin-icon">{getPluginIcon(plugin.pluginId)}</View>
              <Text className="plugin-name">{plugin.pluginName}</Text>
            </View>
          ))}
        </View>
      </View>

      <View className="section">
        <View className="section-header">
          <Text className="section-title">快捷入口</Text>
        </View>
        <View className="quick-links">
          <View className="quick-link" onClick={() => navigateTo('/pages/account/index')}>
            <View className="quick-icon">👤</View>
            <Text className="quick-label">账户管理</Text>
          </View>
          <View className="quick-link" onClick={() => navigateTo('/pages/billing/index')}>
            <View className="quick-icon">📄</View>
            <Text className="quick-label">账单中心</Text>
          </View>
          <View className="quick-link" onClick={() => navigateTo('/pages/ledger/index')}>
            <View className="quick-icon">📊</View>
            <Text className="quick-label">消费记录</Text>
          </View>
          <View className="quick-link" onClick={() => navigateTo('/pages/recharge/index')}>
            <View className="quick-icon">💰</View>
            <Text className="quick-label">充值中心</Text>
          </View>
        </View>
      </View>
    </ScrollView>
  );
}

function getPluginIcon(pluginId: string): string {
  const iconMap: Record<string, string> = {
    [AI_SERVICES.IMAGE_GEN]: '🎨',
    [AI_SERVICES.SCRIPT_GEN]: '📝',
    [AI_SERVICES.TRANSLATE]: '🌐'
  };
  return iconMap[pluginId] || '🔌';
}
