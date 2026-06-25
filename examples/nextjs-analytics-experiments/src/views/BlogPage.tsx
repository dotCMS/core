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

// Stable module-level reference — withExperiments compares config with shallowEqual;
// an inline arrow function would always fail that check, resetting experiment state.
const redirectFn = (url: string) => { window.location.href = url; };

interface BlogPageProps {
  pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

export function BlogPage({ pageContent }: BlogPageProps) {
  const { pageAsset } = useEditableDotCMSPage(pageContent);

  // withExperiments is a hook (useRef + useCallback inside) — must be called here,
  // not at module level. Its useCallback returns the same component reference across
  // renders when config passes shallowEqual, preventing shouldWaitForVariant from resetting.
  const LayoutBodyWithExperiments = withExperiments(DotCMSLayoutBody, {
    ...experimentsConfig,
    redirectFn,
  });

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
