import { useEffect, useState } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor as isInsideEditorFn,
    updateNavigation,
    CUSTOMER_ACTIONS,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSPageContext } from '../models';
export const useDotcmsEditor = (config?: DotCMSPageEditorConfig) => {
    const [isInsideEditor, setIsInsideEditor] = useState(false);
    const [pageInfo, setPageInfo] = useState<DotCMSPageContext['pageAsset'] | null>(null);

    const handlePostMessage = (event: MessageEvent) => {
        const insideEditor = isInsideEditorFn();
        if (!insideEditor) {
            return;
        }

        if (event.data.name === 'SET_PAGE_INFO') {
            setPageInfo(event.data.payload);
        }
    };

    useEffect(() => {
        const insideEditor = isInsideEditorFn();
        if (insideEditor) {
            initEditor();
            updateNavigation(config?.pathname || '/');
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.GET_PAGE_INFO,
                payload: {
                    pathname: config?.pathname
                }
            });
        }

        setIsInsideEditor(insideEditor);
        window.addEventListener('message', handlePostMessage);

        return () => {
            if (insideEditor) {
                destroyEditor();
                window.removeEventListener('message', handlePostMessage);
            }
        };
    }, [config]);

    return { isInsideEditor, pageInfo };
};
