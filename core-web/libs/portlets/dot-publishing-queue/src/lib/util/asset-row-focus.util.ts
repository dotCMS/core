import { ElementRef, Signal, afterRenderEffect, inject, signal } from '@angular/core';

/** The attribute an asset row carries its asset key in, so focus can find it again. */
export const ASSET_ROW_KEY_ATTRIBUTE = 'data-pq-asset-row';

/**
 * Returns focus to an asset's row once its list has reloaded (#38303, FR-039). A contentlet row
 * opens the side panel over the dialog, and closing the panel reloads the list, which empties the
 * table and renders new rows: the row the author left is gone by then. So the row is found again by
 * its asset key, in {@link ASSET_ROW_KEY_ATTRIBUTE}, once the list has loaded and rendered. An asset
 * the reload no longer lists takes no focus.
 *
 * Call it from an injection context: a component field initializer or constructor.
 *
 * @param loaded Whether the list has finished loading, whatever the outcome.
 * @returns Asks for focus on the row of the asset with this key, after the list next loads.
 */
export function injectAssetRowFocusReturn(loaded: Signal<boolean>): (assetKey: string) => void {
    const host = inject<ElementRef<HTMLElement>>(ElementRef);
    const pending = signal<string | null>(null);

    afterRenderEffect(() => {
        const key = pending();

        if (key === null || !loaded()) {
            return;
        }

        pending.set(null);

        const rows = host.nativeElement.querySelectorAll<HTMLElement>(
            `[${ASSET_ROW_KEY_ATTRIBUTE}]`
        );
        Array.from(rows)
            .find((row) => row.getAttribute(ASSET_ROW_KEY_ATTRIBUTE) === key)
            ?.focus();
    });

    return (assetKey) => pending.set(assetKey);
}
