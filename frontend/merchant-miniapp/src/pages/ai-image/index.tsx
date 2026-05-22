import { useState, useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Textarea, Picker, Image, Button } from '@tarojs/components';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { pluginService, AI_SERVICES, IMAGE_SIZES, calculateImageTokenCost } from '@/services/plugin';
import { subscribeService } from '@/services/subscribe';
import './index.scss';

interface GeneratedImage {
  url: string;
  size: string;
}

export default function AiImagePage() {
  const authStore = useAuthStore();
  const accountStore = useAccountStore();
  const [prompt, setPrompt] = useState('');
  const [selectedSize, setSelectedSize] = useState(0);
  const [generating, setGenerating] = useState(false);
  const [generatedImages, setGeneratedImages] = useState<GeneratedImage[]>([]);
  const [tokenUsed, setTokenUsed] = useState(0);

  useEffect(() => {
    checkAuth();
    loadBalance();
  }, []);

  const checkAuth = () => {
    if (!authStore.isAuth) {
      Taro.redirectTo({
        url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/ai-image/index')
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
    if (!prompt.trim()) {
      Taro.showToast({
        title: '请输入提示词',
        icon: 'none'
      });
      return;
    }

    const sizeOption = IMAGE_SIZES[selectedSize];
    const cost = calculateImageTokenCost(sizeOption.value);
    const balance = accountStore.balance?.tokenBalance || 0;

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
      const response = await pluginService.invoke(AI_SERVICES.IMAGE_GEN, {
        prompt,
        image_size: sizeOption.value
      });

      setGeneratedImages([
        {
          url: response.data?.image_url || '',
          size: response.data?.image_size || sizeOption.value
        }
      ]);
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

  const handlePreviewImage = (url: string) => {
    Taro.previewImage({
      urls: [url],
      current: url
    });
  };

  const handleSaveImage = async (url: string) => {
    try {
      const res = await Taro.getImageInfo({ src: url });
      await Taro.saveImageToPhotosAlbum({ filePath: res.path });
      Taro.showToast({
        title: '已保存到相册',
        icon: 'success'
      });
    } catch (error) {
      console.error('保存失败:', error);
      Taro.showToast({
        title: '保存失败，请重试',
        icon: 'none'
      });
    }
  };

  const handleShareImage = async (img: GeneratedImage) => {
    try {
      // 获取图片信息
      const imageInfo = await Taro.getImageInfo({ src: img.url });

      // 记录分享行为
      try {
        await subscribeService.recordShare({
          pluginCode: AI_SERVICES.IMAGE_GEN,
          resultType: 'image',
          resultData: { image_url: img.url, size: img.size },
          resultUrl: img.url,
          formId: Date.now().toString()
        });
      } catch (error) {
        console.error('记录分享失败:', error);
        // 不影响分享流程
      }

      // 获取小程序码
      try {
        const miniCode = await subscribeService.generateMiniappCode(AI_SERVICES.IMAGE_GEN, 'pages/ai-image/index');
        console.log('获取到小程序码:', miniCode.code);

        // 保存小程序码到本地
        const codePath = `${Taro.env.USER_DATA_PATH}/mini_code_${Date.now()}.png`;
        await Taro.downloadFile({
          url: miniCode.code,
          filePath: codePath
        });

        // 分享到微信
        await Taro.shareImageMessage({
          path: imageInfo.path,
          success: () => {
            Taro.showToast({
              title: '分享成功',
              icon: 'success'
            });
          },
          fail: (err) => {
            console.error('分享失败:', err);
            Taro.showToast({
              title: '分享失败',
              icon: 'none'
            });
          }
        });
      } catch (error) {
        console.error('获取小程序码失败:', error);
        // 降级：直接分享图片
        await Taro.shareImageMessage({
          path: imageInfo.path,
          success: () => {
            Taro.showToast({
              title: '分享成功',
              icon: 'success'
            });
          },
          fail: (err) => {
            console.error('分享失败:', err);
            Taro.showToast({
              title: '分享失败',
              icon: 'none'
            });
          }
        });
      }
    } catch (error) {
      console.error('分享图片失败:', error);
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
          <Text className="label">提示词</Text>
          <Textarea
            className="textarea"
            placeholder="描述你想要生成的图片，例如：一件粉色毛衫，简约风格..."
            value={prompt}
            onInput={(e) => setPrompt(e.detail.value)}
            maxlength={500}
            showConfirmBar={false}
          />
          <Text className="char-count">{prompt.length}/500</Text>
        </View>

        <View className="form-item">
          <Text className="label">图片尺寸</Text>
          <Picker
            mode="selector"
            range={IMAGE_SIZES.map(s => `${s.label} (${s.tokens} Tokens)`)}
            value={selectedSize}
            onChange={(e) => setSelectedSize(e.detail.value)}
          >
            <View className="picker">
              <Text>{IMAGE_SIZES[selectedSize].label}</Text>
              <Text className="cost">消耗 {IMAGE_SIZES[selectedSize].tokens} Tokens</Text>
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
        {generating ? '生成中...' : '开始生成'}
      </Button>

      {generatedImages.length > 0 && (
        <View className="result-section">
          <View className="section-header">
            <Text className="section-title">生成结果</Text>
            {tokenUsed > 0 && (
              <Text className="token-used">消耗 {tokenUsed} Tokens</Text>
            )}
          </View>
          <View className="image-grid">
            {generatedImages.map((img, index) => (
              <View key={index} className="image-item">
                <Image
                  src={img.url}
                  mode="aspectFill"
                  className="generated-image"
                  onClick={() => handlePreviewImage(img.url)}
                />
                <View className="image-actions">
                  <Button
                    className="action-btn"
                    size="mini"
                    onClick={() => handlePreviewImage(img.url)}
                  >
                    预览
                  </Button>
                  <Button
                    className="action-btn"
                    size="mini"
                    onClick={() => handleSaveImage(img.url)}
                  >
                    保存
                  </Button>
                  <Button
                    className="action-btn"
                    size="mini"
                    onClick={() => handleShareImage(img)}
                  >
                    分享
                  </Button>
                </View>
              </View>
            ))}
          </View>
        </View>
      )}

      <View className="tips-section">
        <Text className="tips-title">💡 使用提示</Text>
        <Text className="tips-item">• 描述越详细，生成效果越好</Text>
        <Text className="tips-item">• 支持中文和英文提示词</Text>
        <Text className="tips-item">• 大尺寸图片消耗更多 Tokens</Text>
      </View>
    </View>
  );
}
