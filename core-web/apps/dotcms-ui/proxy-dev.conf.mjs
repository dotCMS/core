/* eslint-env es6 */

/**
 * dotCMS backend the dev server proxies `/api` (and friends) to.
 *
 * The host port is dynamic: each `dotwt` worktree runs an isolated BE on a per-worktree port,
 * and `dotwt dev` exports that port as `DOT_BE_PORT` when it starts `nx serve` (see dotwt:
 * "The frontend proxy targets this worktree's BE via DOT_BE_PORT"). Resolution order:
 *   1. `DOTCMS_PROXY_TARGET` — explicit full URL override (e.g. a remote / non-docker BE).
 *   2. `DOT_BE_PORT` — the dotwt per-worktree backend port → `http://localhost:<port>`.
 *   3. Fallback `http://localhost:8080` for the default (non-dotwt) setup.
 */
const target =
    process.env.DOTCMS_PROXY_TARGET ||
    (process.env.DOT_BE_PORT
        ? `http://localhost:${process.env.DOT_BE_PORT}`
        : 'http://localhost:8080');
console.log(`[proxy-dev] proxying dotCMS backend → ${target}`);

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
            '/dotAdmin/logout',
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
