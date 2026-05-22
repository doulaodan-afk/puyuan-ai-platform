import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { billingService, BillItem } from '@/services/billing';
import './index.scss';

export default function BillingPage() {
  const authStore = useAuthStore();
  const [bills, setBills] = useState<BillItem[]>([]);
  const [unpaidAmount, setUnpaidAmount] = useState(0);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<'all' | 'unpaid' | 'paid'>('all');

  useEffect(() => {
    checkAuth();
    loadBills();
  }, [statusFilter]);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/billing/index')
      });
      return;
    }
  };

  const loadBills = async (pageNum: number = 1, isLoadMore: boolean = false) => {
    if (loading) return;

    try {
      setLoading(true);
      const params = {
        page: pageNum,
        pageSize: 20,
        status: statusFilter === 'all' ? undefined : statusFilter === 'unpaid' ? 'pending' : 'paid'
      };
      const response = await billingService.list(params);

      if (isLoadMore) {
        setBills(prev => [...prev, ...response.list]);
      } else {
        setBills(response.list);
      }

      setUnpaidAmount(response.unpaidAmount || 0);
      setHasMore(response.list.length >= response.pageSize);
    } catch (error) {
      console.error('加载失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    setPage(1);
    loadBills(1, false);
  };

  const handleLoadMore = () => {
    if (!hasMore || loading) return;
    const nextPage = page + 1;
    setPage(nextPage);
    loadBills(nextPage, true);
  };

  const handlePayBill = (bill: BillItem) => {
    Taro.showModal({
      title: '确认支付',
      content: `账单号：${bill.billNo}\n金额：¥${bill.amount}`,
      confirmText: '去支付',
      success: (res) => {
        if (res.confirm) {
          // 跳转到支付页面或调用支付
          Taro.showToast({
            title: '支付功能开发中',
            icon: 'none'
          });
        }
      }
    });
  };

  const getStatusClass = (status: string): string => {
    const map: Record<string, string> = {
      pending: 'pending',
      paid: 'paid',
      overdue: 'overdue',
      cancelled: 'cancelled'
    };
    return map[status] || '';
  };

  const formatAmount = (amount: number): string => {
    return `¥${amount.toFixed(2)}`;
  };

  const formatTime = (time: string): string => {
    const date = new Date(time);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  };

  return (
    <View className="page">
      <View className="unpaid-section">
        <Text className="unpaid-label">待支付金额</Text>
        <Text className="unpaid-amount">{formatAmount(unpaidAmount)}</Text>
      </View>

      <View className="filter-bar">
        <View
          className={`filter-item ${statusFilter === 'all' ? 'active' : ''}`}
          onClick={() => setStatusFilter('all')}
        >
          <Text>全部</Text>
        </View>
        <View
          className={`filter-item ${statusFilter === 'unpaid' ? 'active' : ''}`}
          onClick={() => setStatusFilter('unpaid')}
        >
          <Text>待支付</Text>
        </View>
        <View
          className={`filter-item ${statusFilter === 'paid' ? 'active' : ''}`}
          onClick={() => setStatusFilter('paid')}
        >
          <Text>已支付</Text>
        </View>
      </View>

      <ScrollView
        scrollY
        className="content"
        refresherEnabled
        onRefresherRefresh={handleRefresh}
        onScrollToLower={handleLoadMore}
      >
        {bills.length === 0 ? (
          <View className="empty">
            <Text className="empty-icon">📄</Text>
            <Text className="empty-text">暂无账单</Text>
          </View>
        ) : (
          <View className="bill-list">
            {bills.map((bill) => (
              <View key={bill.billNo} className="bill-item">
                <View className="bill-header">
                  <View className="bill-no">账单号：{bill.billNo}</View>
                  <View className={`bill-status ${getStatusClass(bill.status)}`}>
                    {billingService.formatStatus(bill.status)}
                  </View>
                </View>
                <View className="bill-body">
                  <View className="bill-info">
                    <Text className="info-label">账单周期</Text>
                    <Text className="info-value">{billingService.formatPeriod(bill.period)}</Text>
                  </View>
                  <View className="bill-info">
                    <Text className="info-label">创建时间</Text>
                    <Text className="info-value">{formatTime(bill.createdAt)}</Text>
                  </View>
                  {bill.dueDate && (
                    <View className="bill-info">
                      <Text className="info-label">到期时间</Text>
                      <Text className="info-value">{formatTime(bill.dueDate)}</Text>
                    </View>
                  )}
                </View>
                <View className="bill-footer">
                  <Text className="bill-amount">{formatAmount(bill.amount)}</Text>
                  {bill.status === 'pending' && (
                    <View className="pay-btn" onClick={() => handlePayBill(bill)}>
                      立即支付
                    </View>
                  )}
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
