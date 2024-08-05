import { useEffect, useState } from 'react';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    DotCMSPageEditorConfig,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSPageContext } from '../models';

export const useDotcmsLayout = (
    pageAsset: DotCMSPageContext['pageAsset'] | null,
    config: DotCMSPageEditorConfig
) => {
    const [state, setState] = useState({ pageAsset });

    useEffect(() => {
        if (!isInsideEditor()) {
            return;
        }

        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: config?.fetch });
    }, []);

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
