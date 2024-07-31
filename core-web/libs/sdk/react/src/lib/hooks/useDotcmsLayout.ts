import { useEffect, useState } from 'react';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSPageContext } from '../models';

export const useDotcmsLayout = (
    pageAsset: DotCMSPageContext['pageAsset'] | null,
    query?: string
) => {
    const [state, setState] = useState({
        pageAsset,
        isLoading: false
    });

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        if (query) {
            postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_QUERY, payload: query });
        }

        setState((prevState) => ({
            ...prevState,
            isLoading: true
        }));
    }, [query]);

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        const client = DotCmsClient.instance;

        client.editor.on('changes', (data) => {
            const pageAsset = data as DotCMSPageContext['pageAsset'];
            setState({ isLoading: false, pageAsset });
        });

        return () => client.editor.off('changes');
    }, []);

    return state;
};
