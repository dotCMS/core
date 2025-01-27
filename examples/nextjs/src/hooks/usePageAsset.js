import { useEffect, useState } from 'react';

import { client } from '@/utils/dotcmsClient';
import { CLIENT_ACTIONS, getUVEState, postMessageToEditor } from '@dotcms/client';
import { isEditing } from '@/utils/isEditing';

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);
    useEffect(() => {
        if (!isEditing()) {
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
