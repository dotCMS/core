import { defineConfig } from "astro/config";
import react from "@astrojs/react";
import tailwind from "@astrojs/tailwind";

import vercel from "@astrojs/vercel/serverless";

// https://astro.build/config
export default defineConfig({
  integrations: [react(), tailwind()],
  output: "server",
  adapter: vercel(),
  vite: {
    server: {
      proxy: {
        "/dA": "http://localhost:8080",
      },
    },
  },
});
