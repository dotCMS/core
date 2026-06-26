import { getDotCMSPage } from "@/utils/getDotCMSPage";

/** Either a composed page response or the `{ error }` shape on failure. */
export type PageResponse = Awaited<ReturnType<typeof getDotCMSPage>>;

/** Narrows a response to the error branch. */
export function isPageError(
  pageContent: PageResponse,
): pageContent is { error: unknown } {
  return Boolean(pageContent && "error" in pageContent && pageContent.error);
}

/** Best-effort extraction of an HTTP status code from an unknown error. */
export function getErrorStatus(error: unknown): number | undefined {
  if (typeof error === "object" && error !== null && "status" in error) {
    return (error as { status?: number }).status;
  }

  return undefined;
}

/** Page title with a fallback, safe to call on either response branch. */
export function getPageTitle(pageContent: PageResponse, fallback = "Page") {
  if (isPageError(pageContent)) {
    return fallback;
  }

  const page = pageContent.pageAsset?.page;
  return page?.friendlyName || page?.title || fallback;
}
