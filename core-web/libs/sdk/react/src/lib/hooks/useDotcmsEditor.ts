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
