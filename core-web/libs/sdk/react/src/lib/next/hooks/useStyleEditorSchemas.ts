import { useEffect } from 'react';

import { registerStyleEditorSchemas, StyleEditorFormSchema } from '@dotcms/uve';

/**
 * Hook to register style editor forms with the UVE editor.
 * @param forms - Array of style editor form schemas to register
 * @returns void
 */
export const useStyleEditorSchemas = (styleEditorForms: StyleEditorFormSchema[]): void => {
    useEffect(() => {
        registerStyleEditorSchemas(styleEditorForms);
    }, [styleEditorForms]);
};
