import { useEffect, useState } from "react";
import { CLIENT_ACTIONS, postMessageToEditor } from "@dotcms/client";
import { getUVEState } from "@dotcms/uve";
import type { DotCMSPageAsset } from "@dotcms/types";
import { client } from "@utils/client";

export const usePageAsset = (currentPageAsset?: DotCMSPageAsset) => {
  const [pageAsset, setPageAsset] = useState<DotCMSPageAsset | undefined>();

  useEffect(() => {
    if (!getUVEState()) {
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
