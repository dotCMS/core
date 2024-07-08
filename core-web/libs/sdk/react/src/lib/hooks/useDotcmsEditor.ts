import { useEffect, useState } from 'react';

import {
    DotCMSPageEditorConfig,
    destroyEditor,
    initEditor,
    isInsideEditor as isInsideEditorFn,
    updateNavigation
} from '@dotcms/client';

export const useDotcmsEditor = ({ pathname }: DotCMSPageEditorConfig) => {
    const [isInsideEditor, setIsInsideEditor] = useState(false);

    useEffect(() => {
        const insideEditor = isInsideEditorFn();
        if (insideEditor) {
            initEditor({ pathname });
            updateNavigation(pathname || '/');
            setIsInsideEditor(insideEditor);
        }

        return () => {
            if (insideEditor) {
                destroyEditor();
            }
        };
    }, [pathname]);

    return { isInsideEditor };
};
