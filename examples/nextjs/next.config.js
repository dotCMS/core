/** @type {import('next').NextConfig} */

const url = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST);

const nextConfig = {
    reactStrictMode: false,
    images: {
        remotePatterns: [
            {
                protocol: url.protocol.replace(":", ""),
                hostname: url.hostname,
                port: url.port || "",
            },
        ],
        loader: "custom",
        loaderFile: "./src/utils/imageLoader.js",
    },
    async rewrites() {
        return [
            {
                source: "/dA/:path*",
                destination: `${process.env.NEXT_PUBLIC_DOTCMS_HOST}/dA/:path*`,
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

module.exports = nextConfig;
