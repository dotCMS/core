import { getDotCMSPage } from "@/utils/getDotCMSPage";

/** Either a composed page response or the `{ error, graphql }` shape on failure. */
export type PageResponse = Awaited<ReturnType<typeof getDotCMSPage>>;

/** Narrows a response to the error branch. */
export function isPageError(
  pageContent: PageResponse,
): pageContent is Extract<PageResponse, { error: unknown }> {
  return Boolean(pageContent && "error" in pageContent && pageContent.error);
}

/** What `ErrorPage` needs from a failed page request. */
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
  if (typeof error !== "object" || error === null) {
    return {};
  }

  const status =
    "status" in error ? (error as { status?: number }).status : undefined;
  const message =
    process.env.NODE_ENV === "development" && error instanceof Error
      ? error.message
      : undefined;

  return { status, message };
}

/** Page title with a fallback, safe to call on either response branch. */
export function getPageTitle(pageContent: PageResponse, fallback = "Page") {
  if (isPageError(pageContent)) {
    return fallback;
  }

  const page = pageContent.pageAsset?.page;
  return page?.friendlyName || page?.title || fallback;
}
