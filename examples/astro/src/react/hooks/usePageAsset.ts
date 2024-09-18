import type { DotCMSPageAsset } from "@dotcms/types";
import { client } from "@utils/client";
import { useEffect, useState } from "react";

export const usePageAsset = (currentPageAsset: DotCMSPageAsset | undefined) => {
  const [pageAsset, setPageAsset] = useState<DotCMSPageAsset | undefined>();

  useEffect(() => {
    client.editor.on("changes", (page) => {
      if (!page) {
        return;
      }
      setPageAsset(page as DotCMSPageAsset);
    });

    return () => {
      client.editor.off("changes");
    };
  }, [currentPageAsset]);

  return pageAsset ?? currentPageAsset;
};
