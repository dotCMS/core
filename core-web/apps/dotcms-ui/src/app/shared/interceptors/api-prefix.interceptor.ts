import { HttpInterceptorFn } from '@angular/common/http';

const API_VERSION_RELATIVE = /^v\d+\//;
const API_VERSION_ABSOLUTE = /^\/v\d+\//;

/**
 * Ensures that API calls using versioned paths (e.g. `v1/contenttype` or `/v1/contenttype`)
 * are prefixed with `/api/` so they are correctly routed through the dev proxy and
 * resolved against the backend in production.
 *
 * URLs that already start with `/api/`, absolute URLs (`http(s)://`), and
 * non-API paths (`/assets/`, `/html/`, etc.) are left untouched.
 */
export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
    const { url } = req;

    if (url.startsWith('/api/') || url.startsWith('http://') || url.startsWith('https://')) {
        return next(req);
    }

    if (API_VERSION_ABSOLUTE.test(url)) {
        return next(req.clone({ url: `/api${url}` }));
    }

    if (API_VERSION_RELATIVE.test(url)) {
        return next(req.clone({ url: `/api/${url}` }));
    }

    return next(req);
};
