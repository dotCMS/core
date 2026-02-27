/* eslint-env es6 */
/* eslint-disable */

// Backend target: discovered at startup via DOTCMS_HOST env var (set by `just dev-start-frontend`).
// Falls back to :8080 if not set — the conventional fixed port for `just dev-start-on-port 8080`.
const target = process.env.DOTCMS_HOST || 'http://localhost:8080';

export default [
    // 1. Dedicated WebSocket Proxy (Must be first)
    {
        context: ['/api/ws'],
        target,
        ws: true,
        secure: false,
        changeOrigin: true,
        logLevel: 'debug'
    },
    // 2. Main API Proxy
    {
        context: [
            '/api', // Note: /api/ws will be caught by the rule above first
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
            '/image'
        ],
        target,
        secure: false,
        changeOrigin: true,
        logLevel: 'debug',
        timeout: 300000,
        proxyTimeout: 300000,
        ws: false, // Explicitly disable WS here to avoid EPIPE errors on HTTP requests
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
