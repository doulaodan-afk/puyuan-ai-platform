import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { LedgerItem } from '@/services/account';
import './index.scss';

export default function LedgerPage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [entries, setEntries] = useState<LedgerItem[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [entryType, setEntryType] = useState<'all' | 'consume' | 'recharge'>('all');

  useEffect(() => {
    checkAuth();
    loadEntries();
  }, [entryType]);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/ledger/index')
      });
      return;
    }
  };

  const loadEntries = async (pageNum: number = 1, isLoadMore: boolean = false) => {
    if (loading) return;

    try {
      setLoading(true);
      const params = {
        page: pageNum,
        pageSize: 20,
        entryType: entryType === 'all' ? undefined : entryType
      };
      const response = await accountStore.fetchLedger(params);

      if (isLoadMore) {
        setEntries(prev => [...prev, ...response.list]);
      } else {
        setEntries(response.list);
      }

      setHasMore(response.list.length >= response.pageSize);
    } catch (error) {
      console.error('加载失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    setPage(1);
    loadEntries(1, false);
  };

  const handleLoadMore = () => {
    if (!hasMore || loading) return;
    const nextPage = page + 1;
    setPage(nextPage);
    loadEntries(nextPage, true);
  };

  const formatDirection = (direction: string): string => {
    return direction === 'out' ? '-' : '+';
  };

  const formatAmount = (item: LedgerItem): string => {
    const prefix = formatDirection(item.direction);
    return `${prefix}${item.tokenAmount}`;
  };

  const getEntryTypeLabel = (entryType: string): string => {
    const map: Record<string, string> = {
      plugin_consume: '插件消费',
      recharge: '充值',
      refund: '退款',
      bonus: '赠送'
    };
    return map[entryType] || entryType;
  };

  const getPluginName = (pluginId: string): string => {
    const map: Record<string, string> = {
      'ai_image_gen': 'AI 图片生成',
      'ai_script_gen': 'AI 脚本生成',
      'ai_translate': 'AI 跨境翻译'
    };
    return map[pluginId] || pluginId;
  };

  const formatTime = (time: string): string => {
    const date = new Date(time);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      const hours = Math.floor(diff / (1000 * 60 * 60));
      if (hours === 0) {
        const minutes = Math.floor(diff / (1000 * 60));
        return minutes < 1 ? '刚刚' : `${minutes}分钟前`;
      }
      return `${hours}小时前`;
    }
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;

    return `${date.getMonth() + 1}-${date.getDate()}`;
  };

  return (
    <View className="page">
      <View className="filter-bar">
        <View
          className={`filter-item ${entryType === 'all' ? 'active' : ''}`}
          onClick={() => setEntryType('all')}
        >
          <Text>全部</Text>
        </View>
        <View
          className={`filter-item ${entryType === 'consume' ? 'active' : ''}`}
          onClick={() => setEntryType('consume')}
        >
          <Text>消费</Text>
        </View>
        <View
          className={`filter-item ${entryType === 'recharge' ? 'active' : ''}`}
          onClick={() => setEntryType('recharge')}
        >
          <Text>充值</Text>
        </View>
      </View>

      <ScrollView
        scrollY
        className="content"
        refresherEnabled
        onRefresherRefresh={handleRefresh}
        onScrollToLower={handleLoadMore}
      >
        {entries.length === 0 ? (
          <View className="empty">
            <Text className="empty-icon">📊</Text>
            <Text className="empty-text">暂无消费记录</Text>
          </View>
        ) : (
          <View className="entry-list">
            {entries.map((item) => (
              <View key={item.bizNo} className="entry-item">
                <View className="entry-icon">
                  {item.entryType === 'plugin_consume' ? '🎯' : item.entryType === 'recharge' ? '💰' : '📝'}
                </View>
                <View className="entry-info">
                  <Text className="entry-title">
                    {item.entryType === 'plugin_consume' ? getPluginName(item.pluginId) : getEntryTypeLabel(item.entryType)}
                  </Text>
                  <Text className="entry-time">{formatTime(item.occurredAt)}</Text>
                </View>
                <View className="entry-amount">
                  <Text className={`amount ${item.direction === 'out' ? 'out' : 'in'}`}>
                    {formatAmount(item)}
                  </Text>
                  <Text className="unit">Tokens</Text>
                </View>
              </View>
            ))}
            {hasMore && (
              <View className="load-more">
                {loading ? '加载中...' : '上拉加载更多'}
              </View>
            )}
          </View>
        )}
      </ScrollView>
    </View>
  );
}
