import { getDotCMSPage } from '@/utils/getDotCMSPage';

/** Either a composed page response or the `{ error }` shape on failure. */
export type PageResponse = Awaited<ReturnType<typeof getDotCMSPage>>;

/**
 * The success branch of {@link PageResponse} — a resolved page (no `error`).
 * Renderer components receive this after the view has narrowed away the error
 * case, so they can pass it to `useEditableDotCMSPage` without casting.
 */
export type DotCMSPageContent = Exclude<PageResponse, { error: unknown }>;

/** Narrows a response to the error branch. */
export function isPageError(pageContent: PageResponse): pageContent is { error: unknown } {
    return Boolean(pageContent && 'error' in pageContent && pageContent.error);
}

/**
 * Development only, in place of a view's generic copy when a failed request other than a 404
 * has a detail to show: the detail names the cause, so the copy says where the settings live
 * instead of calling it a missing page.
 */
export const DEV_ERROR_COPY = {
    heading: "Couldn't load this page from dotCMS",
    body: 'Check the dotCMS settings in .env.local (VITE_DOTCMS_HOST is the dotcmsUrl).'
} as const;

/** What the error views need from a failed page request. */
export interface ErrorDetails {
    status?: number;
    /** Only set in development; production keeps the generic copy. */
    message?: string;
}

/**
 * Best-effort extraction of an HTTP status code and, in development only, the error message.
 *
 * The message is what names the actual cause (a redirect away from `dotcmsUrl`, a non-JSON
 * response, ...), so a developer sees it without attaching a debugger. It can include URLs
 * and server details, which is why production never receives it.
 */
export function getErrorDetails(error: unknown): ErrorDetails {
    if (typeof error !== 'object' || error === null) {
        return {};
    }

    const status = 'status' in error ? (error as { status?: number }).status : undefined;
    const message = import.meta.env.DEV && error instanceof Error ? error.message : undefined;

    return { status, message };
}

/** Page title with a fallback, safe to call on either response branch. */
export function getPageTitle(pageContent: PageResponse, fallback = 'Page'): string {
    if (isPageError(pageContent)) {
        return fallback;
    }

    const page = pageContent.pageAsset?.page;

    return page?.friendlyName || page?.title || fallback;
}
