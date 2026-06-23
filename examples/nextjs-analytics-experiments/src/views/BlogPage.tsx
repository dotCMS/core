"use client";

import { withExperiments } from "@dotcms/experiments";
import {
  DotCMSLayoutBody,
  useEditableDotCMSPage,
  type DotCMSLayoutBodyProps,
} from "@dotcms/react";
import { useRouter } from "next/navigation";

import { pageComponents } from "@/components/content-types";
import {
  dotCMSMode,
  experimentsConfig,
  experimentsEnabled,
} from "@/config/dotcms.config";

interface BlogPageProps {
  pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

function ExperimentsLayoutBody(props: DotCMSLayoutBodyProps) {
  const { replace } = useRouter();

  if (!experimentsEnabled) {
    return <DotCMSLayoutBody {...props} />;
  }

  /* eslint-disable react-hooks/static-components -- withExperiments must run during render (SDK uses hooks internally) */
  const LayoutBody = withExperiments(DotCMSLayoutBody, {
    ...experimentsConfig,
    redirectFn: replace,
  });

  return <LayoutBody {...props} />;
  /* eslint-enable react-hooks/static-components */
}

/** Blog page with A/B testing via @dotcms/experiments. */
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
