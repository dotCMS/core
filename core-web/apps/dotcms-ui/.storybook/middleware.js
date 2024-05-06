const { createProxyMiddleware } = require('http-proxy-middleware');
/**
 * This is for add those api that are not mocked in the storybook
 * and need to be proxied to the real backend for development purposes
 *
 * @module module.exports
 */
module.exports = function expressMiddleware(router) {
    if (process.env.USE_MIDDLEWARE) {
        // Proxy to the backend to generate text using OpenAI
        router.use(
            '/api/v1/ai/text/generate',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        // Proxy to the backend to generate an image using OpenAI
        router.use(
            '/api/v1/ai/image/generate',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        router.use(
            '/api/v1/ai/image/test',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        // Publish the image_temp generated
        router.use(
            '/api/v1/workflow/actions/default/fire/PUBLISH',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        // Route dotAssets to the backend
        router.use(
            '/da',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        router.use(
            '/api/v2/languages/default/keys',
            createProxyMiddleware({
                target: 'http://localhost:8080',
                changeOrigin: true
            })
        );

        console.info(`\x1b[32m[dotCMS]\x1b[0m`, 'Using middleware for storybook');
    }
};
