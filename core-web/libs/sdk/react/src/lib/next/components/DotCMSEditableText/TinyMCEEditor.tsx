'use client';

import { Editor } from '@tinymce/tinymce-react';

import { DOT_EDITABLE_TEXT_MODE, TINYMCE_CONFIG } from './utils';

/**
 * @internal
 *
 * The TinyMCE editor instance handed back by `onInit`.
 */
export type TinyMCEEditorInstance = NonNullable<Editor['editor']>;

/**
 * @internal
 */
export interface TinyMCEEditorProps {
    /** URL of the TinyMCE script served by the dotCMS host. */
    scriptSrc: string;
    /** Toolbar preset to load: `plain`, `minimal` or `full`. */
    mode: DOT_EDITABLE_TEXT_MODE;
    /** Field content to seed the editor with. */
    initialValue: string;
    /** Called once the editor is ready, with the live editor instance. */
    onEditorInit: (editor: TinyMCEEditorInstance) => void;
    onMouseDown: (event: MouseEvent) => void;
    onFocusOut: () => void;
}

/**
 * @internal
 *
 * Thin wrapper around `@tinymce/tinymce-react`, kept in its own module so it is the *only*
 * place that imports the TinyMCE integration. `DotCMSEditableText` pulls it in with a dynamic
 * import once the UVE actually enters edit mode, so live-mode consumers never download it.
 *
 * Do not import this module statically from anywhere else — doing so puts TinyMCE back in
 * the main bundle and undoes the split.
 */
export function TinyMCEEditor({
    scriptSrc,
    mode,
    initialValue,
    onEditorInit,
    onMouseDown,
    onFocusOut
}: Readonly<TinyMCEEditorProps>) {
    return (
        <Editor
            tinymceScriptSrc={scriptSrc}
            inline={true}
            onInit={(_, editor) => onEditorInit(editor)}
            init={TINYMCE_CONFIG[mode]}
            initialValue={initialValue}
            onMouseDown={onMouseDown}
            onFocusOut={onFocusOut}
        />
    );
}

export default TinyMCEEditor;
