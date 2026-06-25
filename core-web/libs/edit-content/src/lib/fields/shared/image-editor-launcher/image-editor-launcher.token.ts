import { InjectionToken } from '@angular/core';

import { DotImageEditorLauncher } from '@dotcms/image-editor';

export type { DotImageEditorLauncher, ImageEditorOpenParams } from '@dotcms/image-editor';

/**
 * DI seam for launching the image editor from the binary field.
 *
 * The Angular edit-content shell provides the dialog-based launcher. The binary
 * field injects it as `{ optional: true }`; when the token is unprovided (or the
 * launcher's feature flag is off), `onEditImage()` falls back to the legacy Dojo
 * image editor, so no Angular launcher is required for the field to work.
 */
export const IMAGE_EDITOR_LAUNCHER = new InjectionToken<DotImageEditorLauncher>(
    'IMAGE_EDITOR_LAUNCHER'
);
