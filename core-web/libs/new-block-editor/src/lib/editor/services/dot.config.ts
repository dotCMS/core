/**
 * Reserved for future dotCMS editor runtime configuration.
 *
 * HTTP clients in this library call same-origin paths (e.g. `/api/...`, `/dA/...`) and use
 * `withCredentials: true` so requests include the host dotCMS session cookie. Do not hardcode
 * base URLs or bearer tokens here — the editor is expected to run inside an authenticated
 * dotCMS / dotcms-ui origin.
 */
export {};
