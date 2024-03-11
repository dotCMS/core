import { useEffect } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor,
    updateNavigation
} from '@dotcms/client';

export const useDotcmsEditor = (options?: DotCMSPageEditorConfig) => {
    const isInsideEditorPage = isInsideEditor();

    useEffect(() => {
        if (isInsideEditorPage) {
            initEditor(options);
            updateNavigation(options?.pathname || '/');
        }

        return () => {
            if (isInsideEditorPage) {
                destroyEditor();
            }
        };
    }, [isInsideEditorPage, options]);
};
