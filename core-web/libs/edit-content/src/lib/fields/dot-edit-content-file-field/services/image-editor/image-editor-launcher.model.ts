import { Observable } from 'rxjs';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

/**
 * Parameters required to open an image editor for a given field/asset.
 */
export interface ImageEditorOpenParams {
    /** Content inode when editing a published/saved asset. */
    inode?: string;
    /** Temporary file id when editing an unsaved upload. */
    tempId?: string;
    /** Field variable name that scopes editor event routing. */
    variable: string;
    /** Field name passed to the legacy editor (mirrors `variable`). */
    fieldName: string;
}

/**
 * Contract for legacy image editor launchers used by {@link DotFileFieldComponent}.
 *
 * The new editor defaults to the dialog iframe launcher; the web-component bridge
 * uses the Dojo event launcher. A future Angular-native editor will replace the
 * dialog path in `onEditImage()` without changing this interface.
 */
export interface ImageEditorLauncher {
    /**
     * Opens the image editor and emits the edited temp file when the user is done.
     *
     * @param params - Asset/field identifiers used to locate and route the editor.
     * @returns Observable that emits the edited {@link DotCMSTempFile}. It may
     * complete without emitting if the user closes the editor without saving.
     */
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile>;
}
