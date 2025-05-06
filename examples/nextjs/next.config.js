/** @type {import('next').NextConfig} */

const url = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST);

const nextConfig = {
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
                source: '/dA/:path*',
                destination: `${process.env.NEXT_PUBLIC_DOTCMS_HOST}/dA/:path*`,
            },
        ];
    },
};

module.exports = nextConfig;
