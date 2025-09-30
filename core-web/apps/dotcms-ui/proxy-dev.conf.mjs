/* eslint-env es6 */
/* eslint-disable */
export default [
    {
        context: [
            '/api',
            '/c/portal',
            '/html',
            '/dwr',
            '/dA',
            '/dotcms-webcomponents',
            '/DotAjaxDirector',
            '/contentAsset',
            '/application',
            '/assets',
            '/dotcms-block-editor',
            '/dotcms-binary-field-builder',
            '/edit-content-bridge',
            '/categoriesServlet',
            '/JSONTags',
            '/api/vtl',
            '/tinymce',
            '/ext'
        ],
        target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true, // Essential for Firefox compatibility
        logLevel: 'debug',
        timeout: 30000, // 30 second timeout for Firefox
        proxyTimeout: 30000, // Proxy-specific timeout
        ws: true, // Enable WebSocket proxying
        followRedirects: true, // Handle redirects properly
        headers: {
            Connection: 'keep-alive',
            'Cache-Control': 'no-cache'
        },
        pathRewrite: {
            '^/assets/manifest.json': '/dotAdmin/assets/manifest.json',
            '^/assets/monaco-editor/min': '/dotAdmin/assets/monaco-editor/min',
            '^/assets/edit-ema': '/dotAdmin/assets/edit-ema',
            '^/assets/seo': '/dotAdmin/assets/seo',
            '^/assets': '/dotAdmin',
            '^/tinymce': '/dotAdmin/tinymce'
        }
    }
];
