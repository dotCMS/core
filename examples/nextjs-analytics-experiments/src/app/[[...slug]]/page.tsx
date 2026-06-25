import { redirect } from "next/navigation";

import NotFound from "@/app/not-found";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import {
  getErrorStatus,
  getPageTitle,
  isPageError,
} from "@/utils/pageResponse";
import { Page } from "@/views/Page";

interface SlugPageProps {
  params: Promise<{ slug?: string[] }>;
}

function getPath(slug?: string[]) {
  return slug?.length ? `/${slug.join("/")}` : "/";
}

export async function generateMetadata({ params }: SlugPageProps) {
  const { slug } = await params;
  const pageContent = await getDotCMSPage(getPath(slug));

  if (isPageError(pageContent)) {
    return { title: "Error" };
  }

  return { title: getPageTitle(pageContent) };
}

export default async function SlugPage({ params }: SlugPageProps) {
  const { slug } = await params;
  const path = getPath(slug);
  const pageContent = await getDotCMSPage(path);

  if (isPageError(pageContent)) {
    if (getErrorStatus(pageContent.error) === 404) {
      return <NotFound />;
    }

    return (
      <main className="flex min-h-screen items-center justify-center p-8">
        <p className="text-lg text-red-600">Failed to load page: {path}</p>
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

  return <Page pageContent={pageContent} />;
}
