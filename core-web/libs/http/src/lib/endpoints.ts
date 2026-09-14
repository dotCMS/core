/**
 * dotCMS REST paths, in one place so the CLIs cannot drift apart on them.
 *
 * `appconfiguration` is used for the reachability probe rather than `/probes/alive`: the probe
 * endpoints carry IP ACLs and fail from outside the container (dotCMS/core#34509). The same
 * response also carries the instance version, which is what the SDK compatibility warning
 * required by ADR-0019 compares against.
 */
export const DOTCMS_API = {
    /** Reachability probe, and the source of the instance version. */
    appConfiguration: '/api/v1/appconfiguration',
    /** Mints an API token. `expirationDays` is sent as a STRING. */
    apiToken: '/api/v1/authentication/api-token',
    /** Confirms a token actually authorizes, not merely that it was issued. */
    currentUser: '/api/v1/users/current'
} as const;

/** Absolute URL for a dotCMS API path against a normalized instance base URL. */
export function endpoint(baseUrl: string, path: (typeof DOTCMS_API)[keyof typeof DOTCMS_API]) {
    return `${baseUrl}${path}`;
}
