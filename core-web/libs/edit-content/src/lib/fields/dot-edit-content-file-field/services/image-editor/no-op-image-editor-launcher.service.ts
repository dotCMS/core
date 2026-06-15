import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditorLauncher } from './image-editor-launcher.model';

/**
 * No-op {@link ImageEditorLauncher}.
 *
 * Used as the default in hosts where no image editor is wired (the new Angular
 * editor until the GTM launcher ships). It reports the editor as unavailable so
 * the unified field keeps the "Edit image" action hidden, and `open` is a no-op.
 */
@Injectable()
export class NoOpImageEditorLauncher implements ImageEditorLauncher {
    isAvailable(): boolean {
        return false;
    }

    open(): Observable<DotCMSTempFile> {
        return EMPTY;
    }
}
