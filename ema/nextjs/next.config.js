/** @type {import('next').NextConfig} */
const nextConfig = {
    images: {
        remotePatterns: [
            {
                protocol: 'http',
                hostname: 'localhost',
                port: '8080'
            }
        ]
        // remotePatterns: [
        //     {
        //         protocol: 'https',
        //         hostname: '*.dotcms.site',
        //         port: ''
        //     }
        // ]
    }
};

module.exports = nextConfig;
