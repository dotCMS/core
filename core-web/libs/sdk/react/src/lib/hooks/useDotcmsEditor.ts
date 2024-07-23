/* eslint-disable no-console */
import { useEffect, useState } from 'react';

import {
    DotCMSPageEditorConfig,
    DotCmsClient,
    destroyEditor,
    initEditor,
    isInsideEditor as isInsideEditorFn,
    updateNavigation
} from '@dotcms/client';

export const useDotcmsEditor = ({ pathname, onReload }: DotCMSPageEditorConfig) => {
    const [isInsideEditor, setIsInsideEditor] = useState(false);

    useEffect(() => {
        const insideEditor = isInsideEditorFn();

        if (!insideEditor) {
            return;
        }

        initEditor({ pathname });
        updateNavigation(pathname || '/');
        setIsInsideEditor(insideEditor);

        return () => destroyEditor();
    }, [pathname]);

    useEffect(() => {
        const insideEditor = isInsideEditorFn();
        const client = DotCmsClient.instance;

        if (!insideEditor || !onReload) {
            return;
        }

        client.editor.on('changes', () => onReload?.());

        return () => client.editor.off('changes');
    });

    return { isInsideEditor };
};
