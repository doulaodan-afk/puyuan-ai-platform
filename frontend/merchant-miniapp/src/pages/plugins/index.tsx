import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { pluginService, AI_SERVICES, PluginInfo } from '@/services/plugin';
import './index.scss';

export default function PluginsPage() {
  const authStore = useAuthStore();
  const [plugins, setPlugins] = useState<PluginInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'all' | 'enabled'>('enabled');

  useEffect(() => {
    checkAuth();
    loadPlugins();
  }, [activeTab]);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index'
      });
      return;
    }
  };

  const loadPlugins = async () => {
    try {
      setLoading(true);
      const list = await pluginService.list();
      const filtered = activeTab === 'enabled'
        ? list.filter(p => p.status === 1 && p.enabled === 1)
        : list.filter(p => p.status === 1);
      setPlugins(filtered);
    } catch (error) {
      console.error('加载插件失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePluginClick = (plugin: PluginInfo) => {
    const routeMap: Record<string, string> = {
      [AI_SERVICES.IMAGE_GEN]: '/pages/ai-image/index',
      [AI_SERVICES.SCRIPT_GEN]: '/pages/ai-script/index',
      [AI_SERVICES.TRANSLATE]: '/pages/ai-translate/index'
    };

    const route = routeMap[plugin.pluginId];
    if (route) {
      Taro.navigateTo({ url: route });
    } else {
      Taro.showToast({
        title: '该功能正在开发中',
        icon: 'none'
      });
    }
  };

  const getPluginIcon = (pluginId: string): string => {
    const iconMap: Record<string, string> = {
      [AI_SERVICES.IMAGE_GEN]: '🎨',
      [AI_SERVICES.SCRIPT_GEN]: '📝',
      [AI_SERVICES.TRANSLATE]: '🌐'
    };
    return iconMap[pluginId] || '🔌';
  };

  return (
    <View className="page">
      <View className="tabs">
        <View
          className={`tab ${activeTab === 'enabled' ? 'active' : ''}`}
          onClick={() => setActiveTab('enabled')}
        >
          <Text>已启用</Text>
        </View>
        <View
          className={`tab ${activeTab === 'all' ? 'active' : ''}`}
          onClick={() => setActiveTab('all')}
        >
          <Text>全部插件</Text>
        </View>
      </View>

      <ScrollView scrollY className="content">
        {loading ? (
          <View className="loading">加载中...</View>
        ) : plugins.length === 0 ? (
          <View className="empty">
            <Text className="empty-icon">📦</Text>
            <Text className="empty-text">暂无可用插件</Text>
          </View>
        ) : (
          <View className="plugin-list">
            {plugins.map((plugin) => (
              <View
                key={plugin.pluginId}
                className="plugin-item"
                onClick={() => handlePluginClick(plugin)}
              >
                <View className="plugin-icon">{getPluginIcon(plugin.pluginId)}</View>
                <View className="plugin-info">
                  <Text className="plugin-name">{plugin.pluginName}</Text>
                  <Text className="plugin-desc">{plugin.description}</Text>
                  <Text className="plugin-price">{plugin.pricing}</Text>
                </View>
                <View className="plugin-arrow">›</View>
              </View>
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  );
}
