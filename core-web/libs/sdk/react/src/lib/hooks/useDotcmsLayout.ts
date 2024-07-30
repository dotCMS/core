import { useEffect, useState } from 'react';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSPageContext } from '../models';

export const useDotcmsLayout = (
    currentPageAsset: DotCMSPageContext['pageAsset'],
    query?: string
) => {
    const [state, setState] = useState({
        query,
        pageAsset: null,
        isLoading: false
    });

    useEffect(() => {
        if (!query || !isInsideEditor()) {
            return;
        }

        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_QUERY, payload: query });
        setState((prevState) => ({ ...prevState, isLoading: true }));
    }, [query]);

    useEffect(() => {
        const client = DotCmsClient.instance;

        if (!isInsideEditor()) {
            return;
        }

        client.editor.on('changes', (_data) => {
            // console.log('CHANGES', data);
        });

        return () => {
            client.editor.off('changes');
        };
    });

    return state;
};
