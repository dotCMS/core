import type { NextConfig } from "next";

import { dotCMSHost } from "./src/config/dotcms.config";

const url = new URL(dotCMSHost);

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
        destination: `${dotCMSHost}/dA/:path*`,
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
