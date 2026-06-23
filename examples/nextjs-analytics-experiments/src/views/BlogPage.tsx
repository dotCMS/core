"use client";

import { withExperiments } from "@dotcms/experiments";
import {
  DotCMSLayoutBody,
  useEditableDotCMSPage,
  type DotCMSLayoutBodyProps,
} from "@dotcms/react";
import { useRouter } from "next/navigation";

import { pageComponents } from "@/components/content-types";
import { dotCMSMode, experimentsConfig } from "@/config/dotcms.config";

interface BlogPageProps {
  pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

function ExperimentsLayoutBody(props: DotCMSLayoutBodyProps) {
  const { replace } = useRouter();

  /* eslint-disable react-hooks/static-components -- withExperiments uses hooks internally (SDK pattern) */
  const LayoutBody = withExperiments(DotCMSLayoutBody, {
    ...experimentsConfig,
    redirectFn: replace,
  });

  return <LayoutBody {...props} />;
  /* eslint-enable react-hooks/static-components */
}

/**
 * Blog page with A/B testing via @dotcms/experiments.
 *
 * This route always calls `withExperiments` unconditionally — see
 * core-web/libs/sdk/experiments/README.md (anti-patterns section).
 */
export function BlogPage({ pageContent }: BlogPageProps) {
  const { pageAsset } = useEditableDotCMSPage(pageContent);

  return (
    <main className="container mx-auto min-h-screen py-6">
      <ExperimentsLayoutBody
        page={pageAsset}
        components={pageComponents}
        mode={dotCMSMode}
      />
    </main>
  );
}
