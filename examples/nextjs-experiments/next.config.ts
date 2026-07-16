import type { NextConfig } from "next";

import { dotCMSHost } from "./src/config/dotcms.config";

// Fall back to a local default so the app still boots before `.env.local`
// exists — `new URL("")` would otherwise throw `TypeError: Invalid URL` at
// config-load time (matches the guard in `src/utils/imageLoader.ts`).
const resolvedHost = dotCMSHost || "http://localhost:8080";
const url = new URL(resolvedHost);

const nextConfig: NextConfig = {
  // This example is a standalone app; pin the workspace root so Next.js doesn't
  // pick up a lockfile from a parent monorepo directory.
  turbopack: {
    root: __dirname,
  },
  // React Strict Mode double-invokes effects in dev, which conflicts with the
  // Universal Visual Editor (UVE) bridge. Keep it disabled for in-context editing.
  reactStrictMode: false,
  images: {
    remotePatterns: [
      {
        protocol: url.protocol.replace(":", "") as "http" | "https",
        hostname: url.hostname,
        port: url.port || "",
      },
    ],
    loader: "custom",
    loaderFile: "./src/utils/imageLoader.ts",
  },
  async rewrites() {
    return [
      {
        source: "/dA/:path*",
        destination: `${resolvedHost}/dA/:path*`,
      },
    ];
  },
  async redirects() {
    return [
      {
        source: "/:path*/index",
        destination: "/:path*/",
        permanent: true,
      },
    ];
  },
};

export default nextConfig;
