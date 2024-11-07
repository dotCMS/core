import { useEffect, useState } from 'react';

import { client } from '@/utils/dotcmsClient';
import { CLIENT_ACTIONS, isInsideEditor, postMessageToEditor } from '@dotcms/client';

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        // If the page is not found, let the editor know
        if (!currentPageAsset) {
            postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY });

            return;
        }
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
