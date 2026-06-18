import { InjectionToken } from '@angular/core';

import { DotImageEditorLauncher } from '@dotcms/image-editor';

export type { DotImageEditorLauncher, ImageEditorOpenParams } from '@dotcms/image-editor';

/**
 * DI seam for launching the image editor from the binary field.
 *
 * Providers swap implementations (Angular dialog, legacy Dojo bridge, or noop)
 * without the consuming field knowing which editor surfaces the result.
 */
export const IMAGE_EDITOR_LAUNCHER = new InjectionToken<DotImageEditorLauncher>(
    'IMAGE_EDITOR_LAUNCHER'
);
