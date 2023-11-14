const { createProxyMiddleware } = require('http-proxy-middleware');
/**
 * This is for add those api that are not mocked in the storybook
 * and need to be proxied to the real backend for testing purposes
 *
 * @module module.exports
 */
module.exports = function expressMiddleware(router) {
    // Proxy to the backend to generate text using OpenAI
    router.use(
        '/api/ai/text/generate',
        createProxyMiddleware({
            target: 'http://localhost:8080',
            changeOrigin: true,
            pathRewrite: {
                '^/api/ai/text/generate': '/api/ai/text/generate'
            }
        })
    );

    // Proxy to the backend to generate an image using OpenAI
    router.use(
        '/api/ai/image/generate',
        createProxyMiddleware({
            target: 'http://localhost:8080',
            changeOrigin: true,
            pathRewrite: {
                '^/api/ai/image/generate': '/api/ai/image/generate'
            }
        })
    );

    // Publish the image_temp generated
    router.use(
        '/api/v1/workflow/actions/default/fire/PUBLISH',
        createProxyMiddleware({
            target: 'http://localhost:8080',
            changeOrigin: true,
            pathRewrite: {
                '^/api/v1/workflow/actions/default/fire/PUBLISH':
                    '/api/v1/workflow/actions/default/fire/PUBLISH'
            }
        })
    );
};
