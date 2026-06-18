import { InjectionToken } from '@angular/core';

import { DotImageEditorLauncher } from '@dotcms/image-editor';

export type { DotImageEditorLauncher, ImageEditorOpenParams } from '@dotcms/image-editor';

/**
 * DI seam for launching the image editor from the binary field.
 *
 * The Angular edit-content shell provides the dialog-based launcher. When the
 * token is left unprovided, the binary field injects it as optional and hides
 * the "edit image" action, so no fallback implementation is needed.
 */
export const IMAGE_EDITOR_LAUNCHER = new InjectionToken<DotImageEditorLauncher>(
    'IMAGE_EDITOR_LAUNCHER'
);
