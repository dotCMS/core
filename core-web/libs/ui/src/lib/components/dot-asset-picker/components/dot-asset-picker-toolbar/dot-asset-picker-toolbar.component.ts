import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';

import {
    DotCMSBaseTypesContentTypes,
    LOAD_MORE_NODE_TYPE,
    TreeNodeContentData
} from '@dotcms/dotcms-models';

import {
    DotContentTypeFilterComponent,
    DotContentTypeFilterSelection
} from '../../../dot-content-type-filter/dot-content-type-filter.component';
import { DotLanguageFilterComponent } from '../../../dot-language-filter/dot-language-filter.component';
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
        DotContentTypeFilterComponent,
        DotLanguageFilterComponent,
        DotUploadButtonComponent
    ],
    host: { class: 'block w-full' }
})
export class DotAssetPickerToolbarComponent {
    readonly store = inject(DotAssetPickerStore);

    /** Re-emitted to the shell, which owns the upload flow. */
    readonly upload = output<MouseEvent>();

    protected readonly $searchTerm = computed(() => this.store.filters().title ?? '');
    protected readonly $baseTypes = computed(() => this.store.filters().baseType ?? []);
    protected readonly $contentTypes = computed(() => this.store.filters().contentType ?? []);
    protected readonly $languageIds = computed(() =>
        (this.store.filters().languageId ?? []).map(Number)
    );

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

    // Empty selections remove the key rather than setting it to `undefined`, so the filter bag
    // stays clean — same contract as the Content Drive adapters.
    protected onTypeChange({ baseTypes, contentTypes }: DotContentTypeFilterSelection): void {
        if (baseTypes.length) {
            this.store.patchFilters({ baseType: baseTypes });
        } else {
            this.store.removeFilter('baseType');
        }

        if (contentTypes.length) {
            this.store.patchFilters({ contentType: contentTypes });
        } else {
            this.store.removeFilter('contentType');
        }
    }

    protected onLanguageChange(languageIds: number[]): void {
        if (languageIds.length) {
            this.store.patchFilters({ languageId: languageIds.map(String) });
        } else {
            this.store.removeFilter('languageId');
        }
    }
}
