import { useEffect } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor,
    updateNavigation
} from '@dotcms/client';

export const useDotcmsEditor = (config?: DotCMSPageEditorConfig) => {
    const isInsideEditorPage = isInsideEditor();

    useEffect(() => {
        //Here for now, if the comparation is inside initEditor or insideEditor, Next show a Hydration error.
        if (typeof window === 'undefined') {
            return;
        }

        if (isInsideEditorPage) {
            initEditor(config);
            updateNavigation(config?.pathname || '/');
        }

        return () => {
            if (isInsideEditorPage) {
                destroyEditor();
            }
        };
    }, [isInsideEditorPage, config]);
};
