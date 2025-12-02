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
            '/ext',
            "/image"
        ],
        target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true, // Essential for Firefox compatibility
        logLevel: 'debug',
        timeout: 300000, // 5 minute timeout for large file uploads
        proxyTimeout: 300000, // Proxy-specific timeout for large file uploads
        ws: true, // Enable WebSocket proxying for real-time features
        // CRITICAL: followRedirects must be false to avoid 10MB body limit from follow-redirects library
        followRedirects: false,
        headers: {
            Connection: 'keep-alive'
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
