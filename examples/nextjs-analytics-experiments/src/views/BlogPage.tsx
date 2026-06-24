"use client";

// @dotcms/experiments@latest ships index.d.ts pointing to a missing ./src/index path on npm
// @ts-expect-error withExperiments is exported at runtime; types fixed upstream in a follow-up
import { withExperiments } from "@dotcms/experiments";
import {
  DotCMSLayoutBody,
  useEditableDotCMSPage,
} from "@dotcms/react";

import { DevExperimentControls } from "@/components/DevExperimentControls";
import { pageComponents } from "@/components/content-types";
import { dotCMSMode, experimentsConfig } from "@/config/dotcms.config";

// Must be defined at module level — creating it inside a component causes React
// to see a new component type on every render, resetting shouldWaitForVariant
// and keeping content permanently hidden.
const LayoutBodyWithExperiments = withExperiments(DotCMSLayoutBody, {
  ...experimentsConfig,
  redirectFn: (url: string) => {
    window.location.href = url;
  },
});

interface BlogPageProps {
  pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

export function BlogPage({ pageContent }: BlogPageProps) {
  const { pageAsset } = useEditableDotCMSPage(pageContent);

  return (
    <main className="container mx-auto min-h-screen py-6">
      <LayoutBodyWithExperiments
        page={pageAsset}
        components={pageComponents}
        mode={dotCMSMode}
      />
      <DevExperimentControls />
    </main>
  );
}
