import { useEffect, useState } from 'react';

import { client } from '@/utils/dotcmsClient';
import { CLIENT_ACTIONS, isInsideEditor, postMessageToEditor } from '@dotcms/client';

export const usePageAsset = (currentPageAsset, queryMetadata) => {
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
            postMessageToEditor({
                action: CLIENT_ACTIONS.CLIENT_READY,
                payload: {
                    query: queryMetadata.query,
                    variables: queryMetadata.variables
                }
            });

            return;
        }

        return () => {
            client.editor.off('changes');
        };
    }, [currentPageAsset, queryMetadata]);

    return pageAsset ?? currentPageAsset;
};
