// libs/sdk/analytics/vite.config.mts
import { nxViteTsPaths } from "file:///Users/nicobytes1/Code/dotcms/core/core-web/node_modules/@nx/vite/plugins/nx-tsconfig-paths.plugin.js";
import react from "file:///Users/nicobytes1/Code/dotcms/core/core-web/node_modules/@vitejs/plugin-react/dist/index.mjs";
import fs from "fs";
import * as path from "path";
import { defineConfig } from "file:///Users/nicobytes1/Code/dotcms/core/core-web/node_modules/vite/dist/node/index.js";
import dts from "file:///Users/nicobytes1/Code/dotcms/core/core-web/node_modules/vite-plugin-dts/dist/index.mjs";
var __vite_injected_original_dirname = "/Users/nicobytes1/Code/dotcms/core/core-web/libs/sdk/analytics";
var copyReadme = {
  name: "copy-readme",
  writeBundle() {
    fs.copyFileSync(
      path.resolve(__vite_injected_original_dirname, "README.md"),
      path.resolve(__vite_injected_original_dirname, "../../../dist/libs/sdk/analytics/README.md")
    );
  }
};
var vite_config_default = defineConfig({
  root: __vite_injected_original_dirname,
  cacheDir: "../../../node_modules/.vite/libs/sdk/analytics",
  plugins: [
    react(),
    nxViteTsPaths(),
    dts({ entryRoot: "src", tsconfigPath: path.join(__vite_injected_original_dirname, "tsconfig.lib.json") }),
    copyReadme
  ],
  build: {
    outDir: "../../../dist/libs/sdk/analytics",
    emptyOutDir: true,
    reportCompressedSize: true,
    commonjsOptions: {
      transformMixedEsModules: true,
      requireReturnsDefault: "auto"
    },
    lib: {
      entry: {
        index: "src/index.ts",
        "react/index": "src/lib/react/index.ts"
      },
      formats: ["es"]
    },
    rollupOptions: {
      external: ["react", "react-dom", "react/jsx-runtime", "analytics"],
      output: {
        exports: "named",
        preserveModules: true,
        preserveModulesRoot: "src",
        entryFileNames: "[name].js",
        chunkFileNames: "chunks/[name].js"
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsibGlicy9zZGsvYW5hbHl0aWNzL3ZpdGUuY29uZmlnLm10cyJdLAogICJzb3VyY2VzQ29udGVudCI6IFsiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9Vc2Vycy9uaWNvYnl0ZXMxL0NvZGUvZG90Y21zL2NvcmUvY29yZS13ZWIvbGlicy9zZGsvYW5hbHl0aWNzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCIvVXNlcnMvbmljb2J5dGVzMS9Db2RlL2RvdGNtcy9jb3JlL2NvcmUtd2ViL2xpYnMvc2RrL2FuYWx5dGljcy92aXRlLmNvbmZpZy5tdHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL25pY29ieXRlczEvQ29kZS9kb3RjbXMvY29yZS9jb3JlLXdlYi9saWJzL3Nkay9hbmFseXRpY3Mvdml0ZS5jb25maWcubXRzXCI7Ly8vIDxyZWZlcmVuY2UgdHlwZXM9J3ZpdGVzdCcgLz5cbmltcG9ydCB7IG54Vml0ZVRzUGF0aHMgfSBmcm9tICdAbngvdml0ZS9wbHVnaW5zL254LXRzY29uZmlnLXBhdGhzLnBsdWdpbic7XG5pbXBvcnQgcmVhY3QgZnJvbSAnQHZpdGVqcy9wbHVnaW4tcmVhY3QnO1xuaW1wb3J0IGZzIGZyb20gJ2ZzJztcbmltcG9ydCAqIGFzIHBhdGggZnJvbSAncGF0aCc7XG5pbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tICd2aXRlJztcbmltcG9ydCBkdHMgZnJvbSAndml0ZS1wbHVnaW4tZHRzJztcblxuLy8gUGx1Z2luIHNpbXBsZSBwYXJhIGNvcGlhciBSRUFETUUubWRcbmNvbnN0IGNvcHlSZWFkbWUgPSB7XG4gICAgbmFtZTogJ2NvcHktcmVhZG1lJyxcbiAgICB3cml0ZUJ1bmRsZSgpIHtcbiAgICAgICAgZnMuY29weUZpbGVTeW5jKFxuICAgICAgICAgICAgcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJ1JFQURNRS5tZCcpLFxuICAgICAgICAgICAgcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJy4uLy4uLy4uL2Rpc3QvbGlicy9zZGsvYW5hbHl0aWNzL1JFQURNRS5tZCcpXG4gICAgICAgICk7XG4gICAgfVxufTtcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKHtcbiAgICByb290OiBfX2Rpcm5hbWUsXG4gICAgY2FjaGVEaXI6ICcuLi8uLi8uLi9ub2RlX21vZHVsZXMvLnZpdGUvbGlicy9zZGsvYW5hbHl0aWNzJyxcblxuICAgIHBsdWdpbnM6IFtcbiAgICAgICAgcmVhY3QoKSxcbiAgICAgICAgbnhWaXRlVHNQYXRocygpLFxuICAgICAgICBkdHMoeyBlbnRyeVJvb3Q6ICdzcmMnLCB0c2NvbmZpZ1BhdGg6IHBhdGguam9pbihfX2Rpcm5hbWUsICd0c2NvbmZpZy5saWIuanNvbicpIH0pLFxuICAgICAgICBjb3B5UmVhZG1lXG4gICAgXSxcblxuICAgIGJ1aWxkOiB7XG4gICAgICAgIG91dERpcjogJy4uLy4uLy4uL2Rpc3QvbGlicy9zZGsvYW5hbHl0aWNzJyxcbiAgICAgICAgZW1wdHlPdXREaXI6IHRydWUsXG4gICAgICAgIHJlcG9ydENvbXByZXNzZWRTaXplOiB0cnVlLFxuICAgICAgICBjb21tb25qc09wdGlvbnM6IHtcbiAgICAgICAgICAgIHRyYW5zZm9ybU1peGVkRXNNb2R1bGVzOiB0cnVlLFxuICAgICAgICAgICAgcmVxdWlyZVJldHVybnNEZWZhdWx0OiAnYXV0bydcbiAgICAgICAgfSxcbiAgICAgICAgbGliOiB7XG4gICAgICAgICAgICBlbnRyeToge1xuICAgICAgICAgICAgICAgIGluZGV4OiAnc3JjL2luZGV4LnRzJyxcbiAgICAgICAgICAgICAgICAncmVhY3QvaW5kZXgnOiAnc3JjL2xpYi9yZWFjdC9pbmRleC50cydcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBmb3JtYXRzOiBbJ2VzJ11cbiAgICAgICAgfSxcbiAgICAgICAgcm9sbHVwT3B0aW9uczoge1xuICAgICAgICAgICAgZXh0ZXJuYWw6IFsncmVhY3QnLCAncmVhY3QtZG9tJywgJ3JlYWN0L2pzeC1ydW50aW1lJywgJ2FuYWx5dGljcyddLFxuICAgICAgICAgICAgb3V0cHV0OiB7XG4gICAgICAgICAgICAgICAgZXhwb3J0czogJ25hbWVkJyxcbiAgICAgICAgICAgICAgICBwcmVzZXJ2ZU1vZHVsZXM6IHRydWUsXG4gICAgICAgICAgICAgICAgcHJlc2VydmVNb2R1bGVzUm9vdDogJ3NyYycsXG4gICAgICAgICAgICAgICAgZW50cnlGaWxlTmFtZXM6ICdbbmFtZV0uanMnLFxuICAgICAgICAgICAgICAgIGNodW5rRmlsZU5hbWVzOiAnY2h1bmtzL1tuYW1lXS5qcydcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbn0pO1xuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUNBLFNBQVMscUJBQXFCO0FBQzlCLE9BQU8sV0FBVztBQUNsQixPQUFPLFFBQVE7QUFDZixZQUFZLFVBQVU7QUFDdEIsU0FBUyxvQkFBb0I7QUFDN0IsT0FBTyxTQUFTO0FBTmhCLElBQU0sbUNBQW1DO0FBU3pDLElBQU0sYUFBYTtBQUFBLEVBQ2YsTUFBTTtBQUFBLEVBQ04sY0FBYztBQUNWLE9BQUc7QUFBQSxNQUNNLGFBQVEsa0NBQVcsV0FBVztBQUFBLE1BQzlCLGFBQVEsa0NBQVcsNENBQTRDO0FBQUEsSUFDeEU7QUFBQSxFQUNKO0FBQ0o7QUFFQSxJQUFPLHNCQUFRLGFBQWE7QUFBQSxFQUN4QixNQUFNO0FBQUEsRUFDTixVQUFVO0FBQUEsRUFFVixTQUFTO0FBQUEsSUFDTCxNQUFNO0FBQUEsSUFDTixjQUFjO0FBQUEsSUFDZCxJQUFJLEVBQUUsV0FBVyxPQUFPLGNBQW1CLFVBQUssa0NBQVcsbUJBQW1CLEVBQUUsQ0FBQztBQUFBLElBQ2pGO0FBQUEsRUFDSjtBQUFBLEVBRUEsT0FBTztBQUFBLElBQ0gsUUFBUTtBQUFBLElBQ1IsYUFBYTtBQUFBLElBQ2Isc0JBQXNCO0FBQUEsSUFDdEIsaUJBQWlCO0FBQUEsTUFDYix5QkFBeUI7QUFBQSxNQUN6Qix1QkFBdUI7QUFBQSxJQUMzQjtBQUFBLElBQ0EsS0FBSztBQUFBLE1BQ0QsT0FBTztBQUFBLFFBQ0gsT0FBTztBQUFBLFFBQ1AsZUFBZTtBQUFBLE1BQ25CO0FBQUEsTUFDQSxTQUFTLENBQUMsSUFBSTtBQUFBLElBQ2xCO0FBQUEsSUFDQSxlQUFlO0FBQUEsTUFDWCxVQUFVLENBQUMsU0FBUyxhQUFhLHFCQUFxQixXQUFXO0FBQUEsTUFDakUsUUFBUTtBQUFBLFFBQ0osU0FBUztBQUFBLFFBQ1QsaUJBQWlCO0FBQUEsUUFDakIscUJBQXFCO0FBQUEsUUFDckIsZ0JBQWdCO0FBQUEsUUFDaEIsZ0JBQWdCO0FBQUEsTUFDcEI7QUFBQSxJQUNKO0FBQUEsRUFDSjtBQUNKLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
