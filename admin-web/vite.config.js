import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5178,
    proxy: {
      "/login": "http://localhost:8081",
      "/getInfo": "http://localhost:8081",
      "/course": "http://localhost:8081",
      "/study": "http://localhost:8081",
      "/auth": "http://localhost:8081",
      "/actuator": "http://localhost:8081",
      "/swagger-ui.html": "http://localhost:8081",
      "/v3": "http://localhost:8081",
      "/common": "http://localhost:8081",
    },
  },
});
