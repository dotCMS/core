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
    // 2. Embedded dotCMS page proxy (a11y portlet iframe).
    //
    // Lets the portlet iframe load live/edit-mode pages same-origin in dev.
    // Use src="/dot-page/index?mode=EDIT_MODE" — the prefix is stripped so it
    // hits the dotCMS page renderer (e.g. /index) on the BE. The sentinel prefix
    // avoids colliding with the dev server's own Angular routes.
    //
    // DEV-ONLY WORKAROUND FOR A MISSING BACKEND CAPABILITY — do not delete this
    // rule on its own; it is load-bearing (see below).
    // --------------------------------------------------------------------------
    // Why it exists: the Accessibility Studio's side-by-side frames are not
    // passive previews. The run screen reaches INTO each iframe's contentWindow to
    // inject the axe violation-marker overlay and to sync scroll between the two
    // frames (see A11yMarkerService + DotA11yRunComponent.frameWindow). That is
    // same-origin-only by the browser's security model — cross-origin frames throw
    // on contentWindow access, so the markers silently never render.
    //
    // In prod there is no problem: the portlet is served FROM the dotCMS origin, so
    // the page is already same-origin and the iframe needs no prefix at all. This
    // rule exists purely because `nx serve` puts the app on a different origin than
    // the backend, and it papers over that split in the dev server instead of in
    // the platform.
    //
    // What the real fix is (BACKEND): dotCMS should expose a first-class,
    // same-origin endpoint for rendering a page for inspection — i.e. a supported
    // resource under /api that returns the page render, so the Studio (and any
    // future agent that needs to inspect a rendered page) can frame it directly
    // with no origin games and no dev-server rewrite. Today no such endpoint
    // exists, which is the actual gap.
    //
    // Until that lands this rule must stay, and it must stay in sync with
    // DotA11yRunComponent.previewPathPrefix, which emits the `/dot-page` sentinel
    // under isDevMode(). Removing one without the other 404s the preview frames in
    // local dev. Both should be deleted together once the backend endpoint exists.
    {
        context: ['/dot-page'],
        target,
        secure: false,
        changeOrigin: true,
        logLevel: 'debug',
        followRedirects: false,
        pathRewrite: {
            '^/dot-page': ''
        }
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
