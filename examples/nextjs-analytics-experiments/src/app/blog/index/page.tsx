import { redirect } from "next/navigation";

import NotFound from "@/app/not-found";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import {
  getErrorStatus,
  getPageTitle,
  isPageError,
} from "@/utils/pageResponse";
import { BlogPage } from "@/views/BlogPage";

export async function generateMetadata() {
  const pageContent = await getDotCMSPage("/blog/index");

  if (isPageError(pageContent)) {
    return { title: "Blog — Error" };
  }

  return { title: `${getPageTitle(pageContent, "Blog")} — Blog` };
}

export default async function BlogIndexPage() {
  const pageContent = await getDotCMSPage("/blog/index");

  if (isPageError(pageContent)) {
    if (getErrorStatus(pageContent.error) === 404) {
      return <NotFound />;
    }

    return (
      <main className="flex min-h-screen items-center justify-center p-8">
        <p className="text-lg text-red-600">Failed to load blog page.</p>
      </main>
    );
  }

  const vanityUrl = pageContent.pageAsset?.vanityUrl;
  const action = vanityUrl?.action ?? 0;

  if (action > 200 && vanityUrl?.forwardTo) {
    redirect(vanityUrl.forwardTo);
  }

  if (!pageContent.pageAsset) {
    return <NotFound />;
  }

  return <BlogPage pageContent={pageContent} />;
}
