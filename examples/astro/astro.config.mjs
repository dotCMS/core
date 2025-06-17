import { defineConfig } from "astro/config";

import react from "@astrojs/react";
import vercel from "@astrojs/vercel/serverless";
import tailwindcss from "@tailwindcss/vite";
import { getDotCMSHost } from "./src/utils/env";

// https://astro.build/config
export default defineConfig({
  integrations: [react()],
  output: "server",
  adapter: vercel(),
  vite: {
    plugins: [tailwindcss()],
    server: {
      proxy: {
        /* Proxy to dotCMS API for Assets (images, videos, etc.)
          Learn more: https://dev.dotcms.com/docs/image-resizing-and-processing#simpleShortyResize */
        "/dA": getDotCMSHost(),
      },
    },
  },
});
