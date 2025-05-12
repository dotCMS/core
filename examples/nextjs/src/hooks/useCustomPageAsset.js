import { useState, useEffect } from "react";

import { isEditMode } from "@/utils/isEditMode";

import { initUVE, createUVESubscription } from "@dotcms/uve";

/**
 * This hook is used to get the page asset from the UVE. If we are in edit mode, we will get the page asset from the UVE.
 * If we are not in edit mode, we will get the page asset from the initialPageAsset.
 */
export const useCustomPageAsset = (initialPageAsset) => {
    const [pageAsset, setPageAsset] = useState(initialPageAsset || {});

    useEffect(() => {
        if (!isEditMode()) {
            return;
        }

        initUVE();

        createUVESubscription("changes", (pageAsset) => {
            setPageAsset(pageAsset);
        });
    }, []);

    return pageAsset;
};