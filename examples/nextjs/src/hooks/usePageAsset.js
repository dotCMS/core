import { client } from "@/utils/dotcmsClient";
import { useEffect, useState } from "react";

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        client.editor.on("changes", (page) => {
            if (!page) {
                return;
            }
            setPageAsset(page);
        });

        return () => {
            client.editor.off("changes");
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
