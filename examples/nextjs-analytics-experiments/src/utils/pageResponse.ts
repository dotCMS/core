import { getDotCMSPage } from "@/utils/getDotCMSPage";

export type PageResponse = Awaited<ReturnType<typeof getDotCMSPage>>;

export function isPageError(
  pageContent: PageResponse,
): pageContent is { error: unknown } {
  return Boolean(pageContent && "error" in pageContent && pageContent.error);
}

export function getErrorStatus(error: unknown): number | undefined {
  if (typeof error === "object" && error !== null && "status" in error) {
    return (error as { status?: number }).status;
  }

  return undefined;
}

export function getPageTitle(pageContent: PageResponse, fallback = "Page") {
  if (isPageError(pageContent)) {
    return fallback;
  }

  const page = pageContent.pageAsset?.page;
  return page?.friendlyName || page?.title || fallback;
}
