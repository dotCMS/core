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
        setIsInsideEditorPage(isInsideEditor());

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

    return isInsideEditorPage;
};
