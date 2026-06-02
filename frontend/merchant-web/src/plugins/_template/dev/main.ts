import { createApp } from "vue";
import { createPinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import { pluginRoutes } from "../router";
import "./design.css";

const app = createApp(App);
const pinia = createPinia();

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 根路径自动重定向到插件首页
    { path: "/", redirect: "/plugins/{{PLUGIN_ID}}/home" },
    ...pluginRoutes,
  ],
});

app.use(pinia);
app.use(router);
app.use(ElementPlus);

app.mount("#app");