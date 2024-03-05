import { useEffect } from 'react';

import { DotCMSPageEditorConfig, destroyEditor, initEditor, isInsideEditor } from '@dotcms/client';

export const useDotcmsEditor = (options?: DotCMSPageEditorConfig) => {
    const isInsideEditorPage = isInsideEditor();

    useEffect(() => {
        if (isInsideEditorPage) {
            initEditor(options);
        }

        return () => {
            if (isInsideEditorPage) {
                destroyEditor();
            }
        };
    }, [isInsideEditorPage, options]);
};
