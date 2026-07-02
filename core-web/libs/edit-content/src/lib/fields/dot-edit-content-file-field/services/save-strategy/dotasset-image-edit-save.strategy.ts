import { DestroyRef, inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { take } from 'rxjs/operators';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditSaveStrategy } from './image-edit-save-strategy.model';

import { FileFieldStore } from '../../store/file-field.store';
import { getUiMessage } from '../../utils/messages';

/**
 * Image/File field save strategy.
 *
 * Image and File fields store only an `identifier` that references a separate
 * `dotAsset` contentlet (the binary lives in that asset's `asset` field). Saving
 * an edit checks in and publishes a NEW VERSION of the referenced `dotAsset` —
 * preserving version history — via the default PUBLISH workflow action, then
 * refreshes the preview. The field value (the identifier) never changes, so the
 * reference is preserved and other content pointing at the same asset sees the
 * updated image.
 *
 * The edited binary is passed as the staged temp file id in the `asset` field;
 * the check-in resolves it to the real binary server-side.
 *
 * Only ever reached from the new Angular Edit Content: the image-editor entry
 * point for Image/File fields is gated on the presence of the Angular
 * image-editor launcher, so this never runs in the legacy Dojo host.
 */
@Injectable()
export class DotAssetImageEditSaveStrategy implements ImageEditSaveStrategy {
    readonly #store = inject(FileFieldStore);
    readonly #workflowActionsFire = inject(DotWorkflowActionsFireService);
    readonly #destroyRef = inject(DestroyRef);

    apply(tempFile: DotCMSTempFile): void {
        const uploaded = this.#store.uploadedFile();

        // Only reference-backed previews (the resolved dotAsset) can be versioned;
        // guard defensively even though the editor entry point already ensures this.
        if (uploaded?.source !== 'contentlet') {
            return;
        }

        const { identifier, languageId } = uploaded.file;

        this.#workflowActionsFire
            .publishContentletByIdentifier<DotCMSContentlet>(
                { identifier, asset: tempFile.id },
                languageId
            )
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                // Re-hydrate the preview from the new version. The field value (the
                // identifier) is unchanged, so this refreshes the image without
                // touching the reference.
                next: () => this.#store.getAssetData(identifier),
                error: () => this.#store.setUIMessage(getUiMessage('SERVER_ERROR'))
            });
    }
}
