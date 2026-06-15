import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import { installIdentityGuard } from "./plugins/ai-design-assistant/router";
import { useAuthStore } from "./stores/auth";
import "./design.css";
import "./styles.css";

async function bootstrap() {
  const app = createApp(App);
  const pinia = createPinia();

  app.use(pinia);
  app.use(router);
  app.use(ElementPlus);

  // 全局订阅：租户企业名称变化时同步到浏览器 tab 标题
  const auth = useAuthStore();
  auth.$subscribe((_mutation, state) => {
    document.title = state.tenantName ? `濮院毛衫AI平台 + ${state.tenantName}` : '濮院毛衫AI平台';
  });

  // 安装 AI 设计助手身份守卫 - 进入插件前必须选择身份
  installIdentityGuard(router);

  app.mount("#app");
}

bootstrap();
