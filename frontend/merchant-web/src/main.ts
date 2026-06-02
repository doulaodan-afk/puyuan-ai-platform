import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./design.css";
import "./styles.css";
import { installPlugins, loadPlugin } from "./plugins";

async function bootstrap() {
  const app = createApp(App);
  const pinia = createPinia();

  app.use(pinia);
  app.use(router);
  app.use(ElementPlus);

  // 加载开发插件（通过 VITE_DEV_PLUGINS 环境变量指定，逗号分隔）
  // 例: VITE_DEV_PLUGINS=ai-design-assistant,my-plugin
  const devPlugins = import.meta.env.VITE_DEV_PLUGINS as string | undefined;
  if (devPlugins) {
    for (const name of devPlugins.split(",").map((s) => s.trim()).filter(Boolean)) {
      await loadPlugin(app, pinia, router, name, `@/plugins/${name}`);
    }
  }

  // 安装其他已注册的插件
  installPlugins(app, pinia, router);

  app.mount("#app");
}

bootstrap();
