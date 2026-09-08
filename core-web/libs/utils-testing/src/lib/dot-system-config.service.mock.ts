import { Observable, of } from 'rxjs';

import { DotSystemConfig } from '@dotcms/dotcms-models';

/**
 * Minimal `DotSystemConfigService` stand-in for anything that reaches `GlobalStore`.
 *
 * `withSystem`'s `onInit` effect pipes `getSystemConfig()` the moment it flushes, so a
 * bare `mockProvider(DotSystemConfigService)` — which returns `undefined` — makes the
 * feature dereference nothing and throw. rxjs reports that asynchronously: Jest dropped
 * it on the floor, while Vitest counts it as an unhandled error and fails the run. It
 * accounted for 26 of them in edit-content alone, spread over specs that never mention
 * the system config.
 *
 * `null` is `withSystem`'s own initial `systemConfig`, so consuming the mock leaves the
 * store's state exactly as it started.
 */
export const DOT_SYSTEM_CONFIG_SERVICE_MOCK = {
    getSystemConfig: (): Observable<DotSystemConfig | null> => of(null)
};
