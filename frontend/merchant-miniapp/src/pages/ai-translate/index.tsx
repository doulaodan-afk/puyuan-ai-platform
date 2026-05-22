import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Textarea, Picker, Button } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { pluginService, AI_SERVICES, TRANSLATE_LANGS, calculateTranslateTokenCost } from '@/services/plugin';
import { subscribeService } from '@/services/subscribe';
import './index.scss';

export default function AiTranslatePage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [text, setText] = useState('');
  const [selectedLang, setSelectedLang] = useState(0);
  const [translating, setTranslating] = useState(false);
  const [translatedText, setTranslatedText] = useState('');
  const [tokenUsed, setTokenUsed] = useState(0);
  const [estimatedCost, setEstimatedCost] = useState(0);

  useEffect(() => {
    checkAuth();
    loadBalance();
  }, []);

  useEffect(() => {
    updateEstimatedCost();
  }, [text, selectedLang]);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/ai-translate/index')
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

  const updateEstimatedCost = () => {
    const cost = calculateTranslateTokenCost(text.length);
    setEstimatedCost(cost);
  };

  const handleTranslate = async () => {
    if (!text.trim()) {
      Taro.showToast({
        title: '请输入要翻译的文本',
        icon: 'none'
      });
      return;
    }

    const balance = accountStore.balance?.tokenBalance || 0;
    const cost = estimatedCost;

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

    setTranslating(true);
    try {
      const response = await pluginService.invoke(AI_SERVICES.TRANSLATE, {
        text,
        target_lang: TRANSLATE_LANGS[selectedLang].value
      });

      setTranslatedText(response.data?.translated_text || '');
      setTokenUsed(response.token_used);

      // 刷新余额
      await loadBalance();

      Taro.showToast({
        title: '翻译成功',
        icon: 'success'
      });
    } catch (error) {
      console.error('翻译失败:', error);
    } finally {
      setTranslating(false);
    }
  };

  const handleCopyResult = () => {
    if (!translatedText) return;

    Taro.setClipboardData({
      data: translatedText,
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
      content: '确定要清除已翻译的内容吗？',
      success: (res) => {
        if (res.confirm) {
          setText('');
          setTranslatedText('');
          setTokenUsed(0);
          setEstimatedCost(0);
        }
      }
    });
  };

  const handleShareTranslate = async () => {
    if (!translatedText) return;

    try {
      // 记录分享行为
      try {
        await subscribeService.recordShare({
          pluginCode: AI_SERVICES.TRANSLATE,
          resultType: 'text',
          resultData: {
            original_text: text,
            translated_text: translatedText,
            target_lang: TRANSLATE_LANGS[selectedLang].value
          },
          resultUrl: '',
          formId: Date.now().toString()
        });
      } catch (error) {
        console.error('记录分享失败:', error);
        // 不影响分享流程
      }

      // 获取小程序码
      try {
        const miniCode = await subscribeService.generateMiniappCode(AI_SERVICES.TRANSLATE, 'pages/ai-translate/index');
        console.log('获取到小程序码:', miniCode.code);

        // 复制翻译结果到剪贴板作为分享内容
        await Taro.setClipboardData({
          data: `【${TRANSLATE_LANGS[selectedLang].label}翻译】\n${translatedText}`
        });

        Taro.showModal({
          title: '翻译已复制',
          content: '翻译结果已复制到剪贴板，您可以直接分享给好友。',
          confirmText: '知道了',
          showCancel: false
        });
      } catch (error) {
        console.error('获取小程序码失败:', error);
        // 降级：直接复制
        await Taro.setClipboardData({
          data: `【${TRANSLATE_LANGS[selectedLang].label}翻译】\n${translatedText}`
        });

        Taro.showModal({
          title: '翻译已复制',
          content: '翻译结果已复制到剪贴板，您可以直接分享给好友。',
          confirmText: '知道了',
          showCancel: false
        });
      }
    } catch (error) {
      console.error('分享翻译失败:', error);
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
          <Text className="label">待翻译文本</Text>
          <Textarea
            className="textarea"
            placeholder="请输入要翻译的中文文本..."
            value={text}
            onInput={(e) => setText(e.detail.value)}
            maxlength={2000}
            showConfirmBar={false}
            autoHeight
          />
          <View className="input-footer">
            <Text className="char-count">{text.length}/2000</Text>
            {text.length > 0 && (
              <Text className="estimated-cost">预计消耗 {estimatedCost} Tokens</Text>
            )}
          </View>
        </View>

        <View className="form-item">
          <Text className="label">目标语言</Text>
          <Picker
            mode="selector"
            range={TRANSLATE_LANGS.map(l => l.label)}
            value={selectedLang}
            onChange={(e) => setSelectedLang(e.detail.value)}
          >
            <View className="picker">
              <Text>{TRANSLATE_LANGS[selectedLang].label}</Text>
              <Text className="arrow">›</Text>
            </View>
          </Picker>
        </View>
      </View>

      <Button
        className="translate-btn"
        loading={translating}
        disabled={translating || !text.trim()}
        onClick={handleTranslate}
      >
        {translating ? '翻译中...' : '开始翻译'}
      </Button>

      {translatedText && (
        <View className="result-section">
          <View className="section-header">
            <Text className="section-title">翻译结果</Text>
            {tokenUsed > 0 && (
              <Text className="token-used">消耗 {tokenUsed} Tokens</Text>
            )}
          </View>
          <View className="result-content">
            <Text className="result-text">{translatedText}</Text>
          </View>
          <View className="result-actions">
            <Button
              className="action-btn secondary"
              onClick={handleClear}
            >
              清除
            </Button>
            <Button
              className="action-btn"
              onClick={handleCopyResult}
            >
              复制
            </Button>
            <Button
              className="action-btn primary"
              onClick={handleShareTranslate}
            >
              分享
            </Button>
          </View>
        </View>
      )}

      <View className="tips-section">
        <Text className="tips-title">💡 使用提示</Text>
        <Text className="tips-item">• 支持中文到英语、泰语、越南语、马来语、印尼语的翻译</Text>
        <Text className="tips-item">• 计费规则：每 10 字符消耗 1 Token，最少 5 Tokens</Text>
        <Text className="tips-item">• 翻译结果可长按复制</Text>
      </View>
    </View>
  );
}
