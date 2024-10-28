import { useEffect, useState } from "react";
import { client } from "@/utils/dotcmsClient";
import { CUSTOMER_ACTIONS, postMessageToEditor } from "@dotcms/client";

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {

        client.editor.on("changes", (page) => {
            console.log("page", page);
            if (!page) {
                return;
            }
            setPageAsset(page);
        });

        // If the page is not found, let the editor know
        if(!currentPageAsset) {
            postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: {} });

            return;
        }

        return () => {
            client.editor.off("changes");
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
