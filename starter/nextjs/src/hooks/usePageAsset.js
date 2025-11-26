import { useEffect, useState } from 'react';

import { client } from '@/utils/client';
import { CLIENT_ACTIONS, isInsideEditor, postMessageToEditor } from '@dotcms/client';

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        client.editor.on('changes', (page) => {
            if (!page) {
                return;
            }

            setPageAsset(page);
        });

        // If the page is not found, let the editor know
        if (!currentPageAsset) {
            postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY });

            return;
        }

        return () => {
            client.editor.off('changes');
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
