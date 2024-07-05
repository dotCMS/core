import { client } from "@/utils/dotcmsClient";
import { useEffect, useState } from "react";

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        client.page.on("FETCH_PAGE_ASSET_FROM_UVE", (page) => {
            if (!page) {
                return;
            }
            setPageAsset(page);
        });

        return () => {
            client.page.off("FETCH_PAGE_ASSET_FROM_UVE");
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
