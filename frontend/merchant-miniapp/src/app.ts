import { createPinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';
import { useThemeStore } from '@/stores/theme';

const pinia = createPinia();

function App() {
  const themeStore = useThemeStore();

  // 初始化主题
  Taro.onThemeChange(({ theme }) => {
    themeStore.setTheme(theme as 'light' | 'dark');
  });

  // 获取当前系统主题
  Taro.getSystemInfo({
    success: (res) => {
      if (res.theme) {
        themeStore.setTheme(res.theme as 'light' | 'dark');
      }
    }
  });

  // 初始化认证状态
  const authStore = useAuthStore();
  authStore.loadFromStorage();

  return <ConfigProvider theme={themeStore.theme}>{this.props.children}</ConfigProvider>;
}

export default App;
