import { useEffect, useState } from "react";

import { client } from "@/utils/dotcmsClient";
import { CUSTOMER_ACTIONS, isInsideEditor, postMessageToEditor } from "@dotcms/client";

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        client.editor.on("changes", (page) => {
            setIsReady(true);
            setPageAsset(page);
        });

        // If the page is not found, let the editor know
        if (!currentPageAsset) {
            postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY });

            return;
        }

        return () => {
            client.editor.off("changes");
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
