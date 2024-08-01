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
    const [state, setState] = useState({ pageAsset });

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: query });
    }, [query]);

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        const client = DotCmsClient.instance;

        client.editor.on('changes', (data) => {
            const pageAsset = data as DotCMSPageContext['pageAsset'];
            setState({ pageAsset });
        });

        return () => client.editor.off('changes');
    }, []);

    return state;
};
