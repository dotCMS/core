"use client";

import { useContentAnalytics } from "@dotcms/analytics/react";

import { analyticsConfig } from "@/config/dotcms.config";

export function AnalyticsDemoCta() {
  const { track } = useContentAnalytics(analyticsConfig);

  const handleClick = () => {
    track("demo-cta-click", {
      label: "Track demo CTA click",
      location: "header",
    });
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className="rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-900"
    >
      Track demo CTA click
    </button>
  );
}
