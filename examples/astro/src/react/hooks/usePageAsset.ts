import { client } from '../../utils/client';
import { useEffect, useState } from 'react';

export const usePageAsset = (currentPageAsset: any) => {
    const [pageAsset, setPageAsset] = useState(null);

    useEffect(() => {
        client.editor.on('changes', (page: any) => {
            if (!page) {
                return;
            }
            setPageAsset(page);
        });

        return () => {
            client.editor.off('changes');
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
