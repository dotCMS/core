import { signalStoreFeature, type } from '@ngrx/signals';
import { Events, withEventHandlers } from '@ngrx/signals/events';

import { inject, Signal } from '@angular/core';

import { tap } from 'rxjs/operators';

import { ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents } from '../image-editor.events';

/**
 * Download feature: triggers a client-side download of the current preview.
 * Consumes the `previewUrl` selector from {@link withPreview} (so it composes
 * after it). Saving the edited image back to the field is handled in a separate
 * issue and is intentionally not part of this store.
 */
export function withDownload() {
    return signalStoreFeature(
        type<{ state: ImageEditorState; props: { previewUrl: Signal<string> } }>(),
        withEventHandlers((store) => {
            const events = inject(Events);
            const service = inject(DotImageEditorService);

            return {
                download$: events
                    .on(imageEditorLifecycleEvents.downloadRequested)
                    .pipe(
                        tap(() =>
                            service.triggerDownload(
                                store.previewUrl(),
                                store.assetContext().fileName
                            )
                        )
                    )
            };
        })
    );
}
