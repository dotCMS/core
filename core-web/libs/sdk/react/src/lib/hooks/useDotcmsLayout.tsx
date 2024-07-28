import { useEffect, useState } from 'react';

import { CUSTOMER_ACTIONS, graphqlToPageEntity, postMessageToEditor } from '@dotcms/client';

import { DotcmsPageProps } from '../components/DotcmsLayout/DotcmsLayout';

export const useDotcmsLayout = (
    currentPageAsset: DotcmsPageProps['pageContext']['pageAsset'],
    query?: string
) => {
    const [pageAsset, setPageAsset] = useState(currentPageAsset);
    if (query) {
        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_QUERY, payload: query });
    }

    useEffect(() => {
        window.addEventListener('message', (event) => {
            if (event.data.name === 'GRAPHQL_QUERY') {
                const page = graphqlToPageEntity({page: event.data.payload}) as unknown as DotcmsPageProps['pageContext']['pageAsset'];
                s
                setPageAsset(page);
            }
        });
    }, [query]);

    return { pageAsset };
};
