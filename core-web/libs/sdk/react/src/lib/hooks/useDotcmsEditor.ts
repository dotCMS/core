import { useEffect, useState } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor,
    updateNavigation
} from '@dotcms/client';
export const useDotcmsEditor = (config?: DotCMSPageEditorConfig) => {
    const [isInsideEditorPage, setIsInsideEditorPage] = useState(false);
    useEffect(() => {
        const insideEditor = isInsideEditor();
        if (insideEditor) {
            initEditor(config);
            updateNavigation(config?.pathname || '/');
        }

        setIsInsideEditorPage(insideEditor);

        return () => {
            if (insideEditor) {
                destroyEditor();
            }
        };
    }, [config]);

    return isInsideEditorPage;
};
