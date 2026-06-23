"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";

import { pageComponents } from "@/components/content-types";
import { dotCMSMode } from "@/config/dotcms.config";

interface PageProps {
  pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

/** Standard dotCMS page render — analytics only (no experiments). */
export function Page({ pageContent }: PageProps) {
  const { pageAsset } = useEditableDotCMSPage(pageContent);

  return (
    <main className="container mx-auto min-h-screen py-6">
      <DotCMSLayoutBody
        page={pageAsset}
        components={pageComponents}
        mode={dotCMSMode}
      />
    </main>
  );
}
