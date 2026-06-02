import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { accountService, RechargeOrder } from '@/services/account';
import './index.scss';

export default function RechargeOrdersPage() {
  const authStore = useAuthStore();
  const [orders, setOrders] = useState<RechargeOrder[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkAuth();
    loadOrders();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/recharge/orders/index')
      });
      return;
    }
  };

  const loadOrders = async (pageNum: number = 1, isLoadMore: boolean = false) => {
    if (loading) return;

    try {
      setLoading(true);
      const response = await accountService.getOrders({
        page: pageNum,
        pageSize: 20
      });

      if (isLoadMore) {
        setOrders(prev => [...prev, ...response.list]);
      } else {
        setOrders(response.list);
      }

      setHasMore(response.list.length >= 20);
    } catch (error) {
      console.error('加载订单失败:', error);
      Taro.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    setPage(1);
    loadOrders(1, false);
  };

  const handleLoadMore = () => {
    if (!hasMore || loading) return;
    const nextPage = page + 1;
    setPage(nextPage);
    loadOrders(nextPage, true);
  };

  const getPayStatusLabel = (status: string): string => {
    const map: Record<string, string> = {
      PENDING: '待支付',
      SUCCESS: '支付成功',
      FAILED: '支付失败',
      REFUND: '已退款'
    };
    return map[status] || status;
  };

  const getPayChannelLabel = (channel: string): string => {
    const map: Record<string, string> = {
      wechat: '微信支付',
      alipay: '支付宝'
    };
    return map[channel] || channel || '微信支付';
  };

  const formatTime = (time: string): string => {
    const date = new Date(time);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${month}-${day} ${hours}:${minutes}`;
  };

  return (
    <ScrollView
      className="page"
      scrollY
      refresherEnabled
      onRefresherRefresh={handleRefresh}
      onScrollToLower={handleLoadMore}
    >
      {orders.length === 0 ? (
        <View className="empty">
          <Text className="empty-icon">📋</Text>
          <Text className="empty-text">暂无充值记录</Text>
          <View className="empty-action" onClick={() => Taro.navigateBack()}>
            <Text className="action-text">去充值</Text>
          </View>
        </View>
      ) : (
        <View className="order-list">
          {orders.map((order) => (
            <View key={order.orderNo} className="order-card">
              <View className="order-header">
                <Text className="order-no">订单号：{order.orderNo}</Text>
                <Text className={`order-status ${order.payStatus === 'SUCCESS' ? 'success' : order.payStatus === 'PENDING' ? 'pending' : 'failed'}`}>
                  {getPayStatusLabel(order.payStatus)}
                </Text>
              </View>
              <View className="order-body">
                <View className="order-info-row">
                  <Text className="info-label">充值金额</Text>
                  <Text className="info-value amount">¥{order.amount.toFixed(2)}</Text>
                </View>
                <View className="order-info-row">
                  <Text className="info-label">获得 Token</Text>
                  <Text className="info-value">{order.tokenGrant.toLocaleString()} Tokens</Text>
                </View>
                <View className="order-info-row">
                  <Text className="info-label">支付方式</Text>
                  <Text className="info-value">{getPayChannelLabel(order.payChannel)}</Text>
                </View>
                <View className="order-info-row">
                  <Text className="info-label">下单时间</Text>
                  <Text className="info-value">{formatTime(order.createdAt)}</Text>
                </View>
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
  );
}