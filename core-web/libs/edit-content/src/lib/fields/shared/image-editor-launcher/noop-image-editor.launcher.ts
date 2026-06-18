import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotImageEditorLauncher } from '@dotcms/image-editor';

/**
 * Inert launcher used when no image editor is available in the environment.
 *
 * Reports itself unavailable and never opens an editor, so the binary field can
 * hide the "edit image" affordance instead of branching on a missing provider.
 */
@Injectable()
export class NoopImageEditorLauncher implements DotImageEditorLauncher {
    isAvailable(): boolean {
        return false;
    }

    open(): Observable<DotCMSTempFile | null> {
        return of(null);
    }
}
