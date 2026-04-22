import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig(({ command }) => {
  const isBuild = command === "build";
  return {
    plugins: [react()],
    base: "./",
    root: __dirname,
    build: {
      outDir: isBuild
        ? path.resolve(__dirname, "../web/src/wasmJsMain/resources/native")
        : path.resolve(__dirname, "dist"),
      emptyOutDir: true,
      rollupOptions: {
        input: path.resolve(__dirname, "src/main.tsx"),
        output: {
          entryFileNames: "camount-native.js",
          chunkFileNames: "camount-native-[name].js",
          assetFileNames: "camount-native[extname]",
        },
      },
    },
  };
});
