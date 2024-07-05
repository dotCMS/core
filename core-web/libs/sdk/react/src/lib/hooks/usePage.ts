import { useEffect, useState } from 'react';

import { onFetchPageAssetFromUVE } from '@dotcms/client';

import { DotCMSPageContext } from '../models';

export const usePage = (currentPageAsset = {} as DotCMSPageContext['pageAsset']) => {
    const [pageAsset, setPageAsset] = useState<DotCMSPageContext['pageAsset'] | null>(null);

    useEffect(() => {
        const cleanListener = onFetchPageAssetFromUVE((page) => {
            if (!page) {
                return;
            }

            setPageAsset(page as DotCMSPageContext['pageAsset']);
        });

        return () => {
            cleanListener();
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
