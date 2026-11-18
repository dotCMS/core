import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from './constants';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotChipFilterComponent } from '../../../dot-chip-filter/dot-chip-filter.component';
import { DOT_FILTER_FACADE } from '../../filter-facade.token';

/**
 * Toggles whether assets shared across every site (SYSTEM_HOST content) appear in the list, by
 * driving `includeSystemHost` on the search request.
 *
 * On is the default everywhere: the endpoint's own form defaults the flag to true, and both
 * surfaces read an absent key as on. Content Drive additionally seeds the key so the applied state
 * is spelled out in its URL rather than implied by absence; the AssetPicker has no URL and so
 * leaves it absent until the editor touches it. Either way this component reads the same thing —
 * "off only when explicitly off".
 *
 * Shared: it reaches its surface through {@link DOT_FILTER_FACADE} and knows nothing about which
 * store is behind it. That is what lets one chip serve Content Drive and the AssetPicker, and it is
 * why a chip added once shows up in every surface that opts in.
 */
@Component({
    selector: 'dot-shared-assets-filter',
    imports: [DotChipFilterComponent, DotMessagePipe],
    templateUrl: './dot-shared-assets-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Read by each toolbar's canonical-order test. An attribute rather than a class because it
    // identifies the chip rather than styling it.
    host: { 'data-filter-chip': 'sharedAssets' }
})
export class DotSharedAssetsFilterComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);

    protected readonly $enabled = computed(
        () =>
            this.#filters.getFilterValue(SHARED_ASSETS_FILTER_KEY) !== SHARED_ASSETS_DISABLED_VALUE
    );

    /** Flips the toggle, writing the state either way so it is never carried by an absent key. */
    protected onToggle(): void {
        this.#set(this.$enabled() ? SHARED_ASSETS_DISABLED_VALUE : SHARED_ASSETS_ENABLED_VALUE);
    }

    /** The chip's X, which can only mean "turn it off" — it is offered only while on. */
    protected onRemove(): void {
        this.#set(SHARED_ASSETS_DISABLED_VALUE);
    }

    #set(value: string): void {
        this.#filters.patchFilters({ [SHARED_ASSETS_FILTER_KEY]: value });
    }
}
