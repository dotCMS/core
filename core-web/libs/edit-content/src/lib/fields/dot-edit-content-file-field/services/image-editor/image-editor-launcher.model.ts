import { Observable } from 'rxjs';

import { InjectionToken } from '@angular/core';

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
 * Seam used by the unified file field to launch an image editor.
 *
 * Implementations decide whether an editor is available in the current host
 * (legacy JSP/Dojo editor vs. the new Angular editor) and how the editing
 * session is performed. `open` resolves with the edited {@link DotCMSTempFile}.
 */
export interface ImageEditorLauncher {
    /**
     * Whether an image editor is available in the current host.
     *
     * The unified field only renders the "Edit image" action when this returns
     * `true`, keeping the action hidden in hosts that have no editor wired yet.
     */
    isAvailable(): boolean;

    /**
     * Opens the image editor and emits the edited temp file when the user is done.
     *
     * @param params - Asset/field identifiers used to locate and route the editor.
     * @returns Observable that emits the edited {@link DotCMSTempFile}. It may
     * complete without emitting if the user closes the editor without saving.
     */
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile>;
}

/**
 * Injection token for the {@link ImageEditorLauncher} used by the unified file field.
 *
 * Provide {@link NoOpImageEditorLauncher} in hosts without an editor (default in
 * the new Angular editor) and {@link LegacyDojoImageEditorLauncher} in the legacy
 * binary web component.
 */
export const IMAGE_EDITOR_LAUNCHER = new InjectionToken<ImageEditorLauncher>(
    'IMAGE_EDITOR_LAUNCHER'
);
