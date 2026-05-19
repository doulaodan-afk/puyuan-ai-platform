import { createRouter, createWebHistory } from "vue-router";
import { adminRoutes } from "./routes";
import { setupAdminRouterGuards } from "./guards";

const router = createRouter({
  history: createWebHistory(),
  routes: adminRoutes,
});

setupAdminRouterGuards(router);

export default router;
