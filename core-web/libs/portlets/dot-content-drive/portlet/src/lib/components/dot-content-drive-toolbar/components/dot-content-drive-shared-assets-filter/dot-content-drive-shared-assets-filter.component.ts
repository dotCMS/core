import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotChipFilterComponent } from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Toggles whether assets shared across every site (SYSTEM_HOST content) appear in the drive,
 * by driving `includeSystemHost` on the search request.
 *
 * On is the default on both ends: the endpoint's own form defaults the flag to true, and the filter
 * bag is seeded with it (see `withFilterDefaults`) so the applied state is always spelled out in the
 * URL rather than implied by a missing key.
 */
@Component({
    selector: 'dot-content-drive-shared-assets-filter',
    imports: [DotChipFilterComponent, DotMessagePipe],
    templateUrl: './dot-content-drive-shared-assets-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveSharedAssetsFilterComponent {
    readonly #store = inject(DotContentDriveStore);

    protected readonly $enabled = computed(
        () => this.#store.getFilterValue(SHARED_ASSETS_FILTER_KEY) !== SHARED_ASSETS_DISABLED_VALUE
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
        this.#store.patchFilters({ [SHARED_ASSETS_FILTER_KEY]: value });
    }
}
