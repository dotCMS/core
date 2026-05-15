import { inject, Injectable } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import {
    extractContentletPropertiesFromPageAsset,
    updateContentletPropertiesInPageAsset
} from '../../edit-ema-editor/components/dot-uve-palette/utils';
import { ActionPayload } from '../../shared/models';
import { UVEStore } from '../../store/dot-uve.store';
import { PageSnapshot } from '../../store/features/page/withPage';
import { UveIframeMessengerService } from '../iframe-messenger/uve-iframe-messenger.service';

/**
 * Shared service for optimistic page asset updates and rollback extraction.
 *
 * Used by components that implement the optimistic-update + debounced-save + rollback pattern
 * (style editor form, quick edit form). Encapsulates the clone/update/send and strip/extract
 * logic so it is not duplicated across components.
 */
@Injectable()
export class UveOptimisticSaveService {
    readonly #uveStore = inject(UVEStore);
    readonly #iframeMessenger = inject(UveIframeMessengerService);

    /**
     * Optimistically clones the current page asset, applies the given properties
     * to the matching contentlet, updates the store, and sends to iframe.
     * Does NOT save to history — call addCurrentPageToHistory() before the API call.
     */
    updateIframeOptimistically(
        activeContentlet: ActionPayload,
        properties: Record<string, unknown>
    ): void {
        const internalPage = this.#uveStore.pageAsset();

        if (!internalPage || !activeContentlet) {
            return;
        }

        try {
            const cloned = structuredClone(this.#toPlainPageAsset(internalPage));
            const updated = updateContentletPropertiesInPageAsset(
                cloned,
                activeContentlet,
                properties
            );

            this.#uveStore.setPageAsset({ pageAsset: updated });

            const clientResponse = this.#uveStore.pageAsset()?.clientResponse;

            if (clientResponse) {
                this.#iframeMessenger.sendPageData(clientResponse);
            }
        } catch (error) {
            console.error('Error updating iframe optimistically:', error);
        }
    }

    /**
     * Extracts the given field values from the already-rolled-back page asset.
     * Returns the extracted values so the caller can patch or rebuild their form.
     */
    extractFromRollback(
        activeContentlet: ActionPayload,
        fieldNames: string[]
    ): Record<string, unknown> {
        const rolledBackPage = this.#uveStore.pageAsset();

        if (!rolledBackPage || !activeContentlet) {
            return {};
        }

        try {
            const asset = this.#toPlainPageAsset(rolledBackPage);

            return (
                extractContentletPropertiesFromPageAsset(asset, activeContentlet, fieldNames) ?? {}
            );
        } catch (error) {
            console.error('Error extracting from rollback:', error);

            return {};
        }
    }

    /**
     * Strips the computed extras (.content, .requestMetadata, .clientResponse)
     * that are added by the withPage computed and are not part of DotCMSPageAsset.
     */
    #toPlainPageAsset(pageAsset: PageSnapshot): DotCMSPageAsset {
        const asset = { ...pageAsset } as DotCMSPageAsset & {
            content?: unknown;
            requestMetadata?: unknown;
            clientResponse?: unknown;
        };
        delete asset.content;
        delete asset.requestMetadata;
        delete asset.clientResponse;

        return asset as DotCMSPageAsset;
    }
}
