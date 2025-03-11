import { useEffect, useState } from 'react';

import {
    CLIENT_ACTIONS,
    destroyEditor,
    initEditor,
    isInsideEditor as isInsideEditorFn,
    postMessageToEditor,
    updateNavigation
} from '@dotcms/client';
import { createUVESubscription } from '@dotcms/uve';

import { DotcmsPageProps } from '../components/DotcmsLayout/DotcmsLayout';
import { DotCMSPageContext } from '../models';

/**
 * Custom Hook to handle the DotCMS editor interaction with the page.
 *
 * @category Hooks
 * @param {DotcmsPageProps} props {
 *     pageContext,
 *     config,
 * }
 * @returns {DotCMSPageContext} The context for a DotCMS page provided by the editor.
 */
export const useDotcmsEditor = ({ pageContext, config }: DotcmsPageProps) => {
    const { pathname, onReload, editor } = config;
    const [state, setState] = useState<DotCMSPageContext>({
        ...pageContext,
        isInsideEditor: false
    });

    /**
     * Initializes the DotCMS editor.
     */
    useEffect(() => {
        if (!isInsideEditorFn()) {
            return;
        }

        initEditor({ pathname });
        updateNavigation(pathname || '/');
        setState((prevState) => ({ ...prevState, isInsideEditor: true }));

        return () => destroyEditor();
    }, [pathname]);

    /**
     * Reloads the page when changes are made in the editor.
     */
    useEffect(() => {
        const insideEditor = isInsideEditorFn();

        if (!insideEditor || !onReload) {
            return;
        }

        const { unsubscribe } = createUVESubscription('changes', () => onReload());

        return () => unsubscribe();
    }, [onReload]);

    /**
     * Sends a message to the editor when the client is ready.
     */
    useEffect(() => {
        if (!isInsideEditorFn()) {
            return;
        }

        postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY, payload: editor });
    }, [pathname, editor]);

    /**
     * Old
     */
    useEffect(() => {
        if (!isInsideEditorFn()) {
            return;
        }

        const { unsubscribe } = createUVESubscription('changes', (data) => {
            const pageAsset = data as DotCMSPageContext['pageAsset'];
            setState((state) => ({ ...state, pageAsset }));
        });

        return () => unsubscribe();
    }, []);

    return state;
};
