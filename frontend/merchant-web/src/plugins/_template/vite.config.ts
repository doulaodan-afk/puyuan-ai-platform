import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// {{PLUGIN_ID}} 插件独立调试配置
// 运行方式: cd src/plugins/{{PLUGIN_ID}} && npm run dev
export default defineConfig({
  plugins: [vue()],
  root: path.resolve(__dirname, "dev"),
  resolve: {
    alias: [
      // 重映射 @/stores/auth 到 stub（独立调试不需要真实登录）
      { find: "@/stores/auth", replacement: path.resolve(__dirname, "dev/stores/auth.ts") },
      // 插件内部 @ 指向插件根目录
      { find: "@", replacement: path.resolve(__dirname, ".") },
    ],
  },
  server: {
    port: 5181,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
    },
  },
});