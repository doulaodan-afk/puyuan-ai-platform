import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Textarea, Input, Picker, Button } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { pluginService, AI_SERVICES, SCRIPT_TYPES } from '@/services/plugin';
import { subscribeService } from '@/services/subscribe';
import './index.scss';

export default function AiScriptPage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [productDesc, setProductDesc] = useState('');
  const [productUrl, setProductUrl] = useState('');
  const [selectedType, setSelectedType] = useState(0);
  const [generating, setGenerating] = useState(false);
  const [generatedScript, setGeneratedScript] = useState('');
  const [tokenUsed, setTokenUsed] = useState(0);

  useEffect(() => {
    checkAuth();
    loadBalance();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/ai-script/index')
      });
      return;
    }
    if (!authStore.canAccessTools) {
      Taro.showToast({
        title: '您没有权限使用此功能',
        icon: 'none'
      });
      setTimeout(() => {
        Taro.navigateBack();
      }, 1500);
    }
  };

  const loadBalance = async () => {
    await accountStore.fetchBalance();
  };

  const handleGenerate = async () => {
    if (!productDesc.trim()) {
      Taro.showToast({
        title: '请输入商品描述',
        icon: 'none'
      });
      return;
    }

    const balance = accountStore.balance?.tokenBalance || 0;
    const cost = 20;

    if (cost > balance) {
      Taro.showModal({
        title: '余额不足',
        content: '您的 Token 余额不足，请先充值',
        confirmText: '去充值',
        success: (res) => {
          if (res.confirm) {
            Taro.navigateTo({ url: '/pages/recharge/index' });
          }
        }
      });
      return;
    }

    setGenerating(true);
    try {
      const response = await pluginService.invoke(AI_SERVICES.SCRIPT_GEN, {
        product_desc: productDesc,
        product_url: productUrl || undefined,
        script_type: SCRIPT_TYPES[selectedType].value
      });

      setGeneratedScript(response.data?.script || '');
      setTokenUsed(response.token_used);

      // 刷新余额
      await loadBalance();

      Taro.showToast({
        title: '生成成功',
        icon: 'success'
      });
    } catch (error) {
      console.error('生成失败:', error);
    } finally {
      setGenerating(false);
    }
  };

  const handleCopyScript = () => {
    if (!generatedScript) return;

    Taro.setClipboardData({
      data: generatedScript,
      success: () => {
        Taro.showToast({
          title: '已复制到剪贴板',
          icon: 'success'
        });
      }
    });
  };

  const handleClear = () => {
    Taro.showModal({
      title: '确认清除',
      content: '确定要清除已生成的脚本吗？',
      success: (res) => {
        if (res.confirm) {
          setGeneratedScript('');
          setTokenUsed(0);
        }
      }
    });
  };

  const handleShareScript = async () => {
    if (!generatedScript) return;

    try {
      // 记录分享行为
      try {
        await subscribeService.recordShare({
          pluginCode: AI_SERVICES.SCRIPT_GEN,
          resultType: 'script',
          resultData: { script: generatedScript },
          resultUrl: '',
          formId: Date.now().toString()
        });
      } catch (error) {
        console.error('记录分享失败:', error);
        // 不影响分享流程
      }

      // 获取小程序码
      try {
        const miniCode = await subscribeService.generateMiniappCode(AI_SERVICES.SCRIPT_GEN, 'pages/ai-script/index');
        console.log('获取到小程序码:', miniCode.code);

        // 复制脚本到剪贴板作为分享内容
        await Taro.setClipboardData({
          data: generatedScript
        });

        Taro.showModal({
          title: '脚本已复制',
          content: '脚本内容已复制到剪贴板，您可以直接分享给好友。',
          confirmText: '知道了',
          showCancel: false
        });
      } catch (error) {
        console.error('获取小程序码失败:', error);
        // 降级：直接复制
        await Taro.setClipboardData({
          data: generatedScript
        });

        Taro.showModal({
          title: '脚本已复制',
          content: '脚本内容已复制到剪贴板，您可以直接分享给好友。',
          confirmText: '知道了',
          showCancel: false
        });
      }
    } catch (error) {
      console.error('分享脚本失败:', error);
      Taro.showToast({
        title: '分享失败，请重试',
        icon: 'none'
      });
    }
  };

  return (
    <View className="page">
      <View className="balance-bar">
        <Text className="balance-label">可用余额：</Text>
        <Text className="balance-amount">{accountStore.balanceText}</Text>
        <Text className="balance-unit">Tokens</Text>
      </View>

      <View className="form-section">
        <View className="form-item">
          <Text className="label">商品描述 *</Text>
          <Textarea
            className="textarea"
            placeholder="描述您的商品，例如：粉色圆领毛衫，采用优质羊毛..."
            value={productDesc}
            onInput={(e) => setProductDesc(e.detail.value)}
            maxlength={1000}
            showConfirmBar={false}
          />
          <Text className="char-count">{productDesc.length}/1000</Text>
        </View>

        <View className="form-item">
          <Text className="label">商品链接（可选）</Text>
          <Input
            className="input"
            placeholder="如有商品链接，可在此粘贴"
            value={productUrl}
            onInput={(e) => setProductUrl(e.detail.value)}
          />
        </View>

        <View className="form-item">
          <Text className="label">脚本类型</Text>
          <Picker
            mode="selector"
            range={SCRIPT_TYPES.map(t => t.label)}
            value={selectedType}
            onChange={(e) => setSelectedType(e.detail.value)}
          >
            <View className="picker">
              <Text>{SCRIPT_TYPES[selectedType].label}</Text>
              <Text className="arrow">›</Text>
            </View>
          </Picker>
        </View>
      </View>

      <Button
        className="generate-btn"
        loading={generating}
        disabled={generating}
        onClick={handleGenerate}
      >
        {generating ? '生成中...' : '生成脚本 (20 Tokens)'}
      </Button>

      {generatedScript && (
        <View className="result-section">
          <View className="section-header">
            <Text className="section-title">生成结果</Text>
            {tokenUsed > 0 && (
              <Text className="token-used">消耗 {tokenUsed} Tokens</Text>
            )}
          </View>
          <View className="script-content">
            <Text className="script-text">{generatedScript}</Text>
          </View>
          <View className="script-actions">
            <Button
              className="action-btn secondary"
              onClick={handleClear}
            >
              清除
            </Button>
            <Button
              className="action-btn"
              onClick={handleCopyScript}
            >
              复制
            </Button>
            <Button
              className="action-btn primary"
              onClick={handleShareScript}
            >
              分享
            </Button>
          </View>
        </View>
      )}

      <View className="tips-section">
        <Text className="tips-title">💡 使用提示</Text>
        <Text className="tips-item">• 详细描述商品特点，脚本会更精准</Text>
        <Text className="tips-item">• 可提供商品链接，AI 将分析详情</Text>
        <Text className="tips-item">• 脚本生成固定消耗 20 Tokens</Text>
      </View>
    </View>
  );
}
