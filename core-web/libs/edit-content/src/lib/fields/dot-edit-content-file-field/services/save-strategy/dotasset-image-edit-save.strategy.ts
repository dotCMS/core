import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditSaveStrategy } from './image-edit-save-strategy.model';

/**
 * Image/File field save strategy.
 *
 * Image and File fields store only an `identifier` that references a separate
 * `dotAsset` contentlet (the binary lives in that asset's `asset` field). Saving
 * an edit must check in a NEW VERSION of the referenced `dotAsset` — preserving
 * version history — and refresh the preview WITHOUT mutating the field value,
 * which must keep pointing at the same identifier.
 *
 * Scaffolded intentionally: the versioned check-in of the referenced asset is
 * implemented in a follow-up step. It deliberately does NOT fall back to the
 * Binary inline write, which would overwrite the field's identifier reference
 * with a temp id and corrupt the reference.
 *
 * Only ever reached from the new Angular Edit Content: the image-editor entry
 * point for Image/File fields is gated on the presence of the Angular
 * image-editor launcher, so this never runs in the legacy Dojo host.
 */
@Injectable()
export class DotAssetImageEditSaveStrategy implements ImageEditSaveStrategy {
    apply(_tempFile: DotCMSTempFile): void {
        // TODO(#36363): upload/reuse the edited temp file and check in a new
        // version of the referenced dotAsset (via the workflow fire endpoint),
        // then re-hydrate the preview without changing the field value.
    }
}
