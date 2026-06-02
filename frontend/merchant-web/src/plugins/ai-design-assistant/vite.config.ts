import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

export default defineConfig({
  plugins: [vue()],
  root: path.resolve(__dirname, "dev"),
  resolve: {
    alias: [
      // 更精确的别名优先匹配：重映射 @/stores/auth 到 stub
      { find: "@/stores/auth", replacement: path.resolve(__dirname, "dev/stores/auth.ts") },
      // 插件内部 @ 指向插件根目录（dev 目录的上一级）
      { find: "@", replacement: path.resolve(__dirname, ".") },
    ],
  },
  server: {
    port: 5180,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
    },
  },
});