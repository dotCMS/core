import type { NextConfig } from "next";

const dotcmsHost =
  process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "http://localhost:8080";
const url = new URL(dotcmsHost);

const nextConfig: NextConfig = {
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
        destination: `${dotcmsHost}/dA/:path*`,
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
