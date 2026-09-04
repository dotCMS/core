import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';

import {
    DotCMSBaseTypesContentTypes,
    LOAD_MORE_NODE_TYPE,
    TreeNodeContentData
} from '@dotcms/dotcms-models';

import { DotContentTypeFilterChipComponent } from '../../../dot-filter-bar/chips/dot-content-type-filter-chip/dot-content-type-filter-chip.component';
import { DotLanguageFilterChipComponent } from '../../../dot-filter-bar/chips/dot-language-filter-chip/dot-language-filter-chip.component';
import { DotSharedAssetsFilterComponent } from '../../../dot-filter-bar/chips/dot-shared-assets-filter/dot-shared-assets-filter.component';
import { DotFilterBarComponent } from '../../../dot-filter-bar/dot-filter-bar.component';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotUploadButtonComponent } from '../../../dot-upload-button/dot-upload-button.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * AssetPicker toolbar: search, type and locale filters, and the upload trigger.
 *
 * Pure store wiring over the pieces `@dotcms/ui` already shares with Content Drive. It deliberately
 * leaves out everything the picker has no use for — Add New, workflow actions and filters, and the
 * user-searchable field chips.
 */
@Component({
    selector: 'dot-asset-picker-toolbar',
    templateUrl: './dot-asset-picker-toolbar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotSearchInputComponent,
        DotSharedAssetsFilterComponent,
        DotFilterBarComponent,
        DotContentTypeFilterChipComponent,
        DotLanguageFilterChipComponent,
        DotUploadButtonComponent
    ],
    host: { class: 'block w-full' }
})
export class DotAssetPickerToolbarComponent {
    readonly store = inject(DotAssetPickerStore);

    /** Re-emitted to the shell, which owns the upload flow. */
    readonly upload = output<MouseEvent>();

    protected readonly $searchTerm = computed(() => this.store.filters().title ?? '');
    /**
     * Restricts the content-type selector to what the entry point allows — neither a File nor an
     * Image field may offer Widget or Content. `null` means "no restriction".
     *
     * Reads `allowedBaseTypes`, never `baseTypes`: the latter is only the *pre-selection*, and a
     * File field legitimately has none while still being restricted.
     */
    protected readonly $allowedBaseTypes = computed(() => {
        const allowedBaseTypes = this.store.config()?.allowedBaseTypes;

        return allowedBaseTypes?.length
            ? (allowedBaseTypes as DotCMSBaseTypesContentTypes[])
            : null;
    });

    /** Folder-pinned upload preference, which drives the button's label. */
    protected readonly $defaultBaseType = computed(() => {
        const data = this.store.selectedNode()?.data;

        return data && data.type !== LOAD_MORE_NODE_TYPE
            ? ((data as TreeNodeContentData).defaultBaseType ?? null)
            : null;
    });
}
