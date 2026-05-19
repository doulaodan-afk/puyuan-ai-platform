import { createRouter, createWebHistory } from "vue-router";
import { merchantRoutes } from "./routes";
import { setupRouterGuards } from "./guards";

const router = createRouter({
  history: createWebHistory(),
  routes: merchantRoutes,
});

setupRouterGuards(router);

export default router;