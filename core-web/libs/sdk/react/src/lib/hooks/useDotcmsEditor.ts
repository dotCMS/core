import { useEffect, useState } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor,
    updateNavigation,
    CUSTOMER_ACTIONS,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSPageContext } from '../models';
export const useDotcmsEditor = (config?: DotCMSPageEditorConfig) => {
    const [isInsideEditorPage, setIsInsideEditorPage] = useState(false);
    const [pageInfo, setPageInfo] = useState<DotCMSPageContext['pageAsset'] | null>(null);

    const handlePostMessage = (event: MessageEvent) => {
        const insideEditor = isInsideEditor();
        if (!insideEditor) {
            return;
        }

        if (event.data.name === 'SET_PAGE_INFO') {
            setPageInfo(event.data.payload);
        }
    };

    useEffect(() => {
        const insideEditor = isInsideEditor();
        if (insideEditor) {
            initEditor(config);
            updateNavigation(config?.pathname || '/');
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.GET_PAGE_INFO,
                payload: {
                    pathname: config?.pathname
                }
            });
        }

        setIsInsideEditorPage(insideEditor);
        window.addEventListener('message', handlePostMessage);

        return () => {
            if (insideEditor) {
                destroyEditor();
                window.removeEventListener('message', handlePostMessage);
            }
        };
    }, [config]);

    return { isInsideEditorPage, pageInfo };
};
