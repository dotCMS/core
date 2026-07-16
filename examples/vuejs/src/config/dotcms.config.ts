import type { DotCMSPageRendererMode } from '@dotcms/types';

/**
 * Centralized, typed access to the dotCMS environment variables. Every other
 * module imports from here instead of reading `import.meta.env` directly.
 *
 * All variables are `VITE_*` (exposed to the browser) because the example uses a
 * read-only API token that is safe to expose and required by the UVE bridge.
 */
export const dotCMSHost = import.meta.env.VITE_DOTCMS_HOST ?? '';
export const dotCMSAuthToken = import.meta.env.VITE_DOTCMS_AUTH_TOKEN ?? '';
export const dotCMSSiteId = import.meta.env.VITE_DOTCMS_SITE_ID ?? '';

export const dotCMSMode = (import.meta.env.VITE_DOTCMS_MODE ??
    'production') as DotCMSPageRendererMode;

/** Verbose client logging in development, quiet otherwise. */
export const dotCMSLogLevel = import.meta.env.DEV ? 'verbose' : 'default';
