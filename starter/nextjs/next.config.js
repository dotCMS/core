/** @type {import('next').NextConfig} */

const url = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST);

const nextConfig = {
    images: {
        remotePatterns: [
            {
                protocol: url.protocol.slice(0, -1),
                hostname: url.hostname,
                port: url.port,
                pathname: '/dA/**',
            },
        ],
    },
};

module.exports = nextConfig;
