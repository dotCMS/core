import { useEffect, useState } from "react";
import {
  CLIENT_ACTIONS,
  isInsideEditor,
  postMessageToEditor,
} from "@dotcms/client";
import type { DotCMSPageAsset } from "@dotcms/types";
import { client } from "@utils/client";

export const usePageAsset = (currentPageAsset: DotCMSPageAsset | undefined) => {
  const [pageAsset, setPageAsset] = useState<DotCMSPageAsset | undefined>();

  useEffect(() => {
    if (!isInsideEditor()) {
      return;
    }

    client.editor.on("changes", (page) => {
      if (!page) {
        return;
      }
      setPageAsset(page as DotCMSPageAsset);
    });

    // If the page is not found, let the editor know
    if (!currentPageAsset) {
      postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY });

      return;
    }

    return () => {
      client.editor.off("changes");
    };
  }, [currentPageAsset]);

  return pageAsset ?? currentPageAsset;
};
