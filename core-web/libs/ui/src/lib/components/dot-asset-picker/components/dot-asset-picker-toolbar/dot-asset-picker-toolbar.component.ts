import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentTypeField,
    LOAD_MORE_NODE_TYPE,
    TreeNodeContentData
} from '@dotcms/dotcms-models';

import { DotContentTypeFilterChipComponent } from '../../../dot-filter-bar/chips/dot-content-type-filter-chip/dot-content-type-filter-chip.component';
import { DotFieldFilterComponent } from '../../../dot-filter-bar/chips/dot-field-filter/dot-field-filter.component';
import { DotFieldFilterMenuComponent } from '../../../dot-filter-bar/chips/dot-field-filter-menu/dot-field-filter-menu.component';
import { DotLanguageFilterChipComponent } from '../../../dot-filter-bar/chips/dot-language-filter-chip/dot-language-filter-chip.component';
import { DotSharedAssetsFilterComponent } from '../../../dot-filter-bar/chips/dot-shared-assets-filter/dot-shared-assets-filter.component';
import {
    DotContentStatus,
    PUBLISHED_ONLY_STATUSES
} from '../../../dot-filter-bar/chips/dot-status-filter/constants';
import { DotStatusFilterComponent } from '../../../dot-filter-bar/chips/dot-status-filter/dot-status-filter.component';
import { DotFilterBarComponent } from '../../../dot-filter-bar/dot-filter-bar.component';
import { DotFilterChipError } from '../../../dot-filter-bar/filter-facade.token';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotUploadButtonComponent } from '../../../dot-upload-button/dot-upload-button.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * AssetPicker toolbar: search, the shared filter row, and the upload trigger.
 *
 * Pure store wiring over the pieces `@dotcms/ui` already shares with Content Drive — the filters
 * themselves read and write through `DOT_FILTER_FACADE` and need nothing from here. What this
 * component still owns is the two things only the picker knows: how its caller's restrictions bound
 * a control's options, and what the Upload button should be labelled.
 *
 * It deliberately offers no create-content affordance and never enters Content Drive's
 * selection-driven action mode: the picker confirms a single selection through its own dialog
 * footer (FR-008, FR-017).
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
        DotStatusFilterComponent,
        DotLanguageFilterChipComponent,
        DotFieldFilterComponent,
        DotFieldFilterMenuComponent,
        DotUploadButtonComponent
    ],
    host: { class: 'block w-full' }
})
export class DotAssetPickerToolbarComponent {
    readonly store = inject(DotAssetPickerStore);

    /** Re-emitted to the shell, which owns the upload flow. */
    readonly upload = output<MouseEvent>();

    /**
     * A filter chip could not load its options.
     *
     * Re-emitted rather than handled: the shell owns the dialog's toast, which is the only error
     * channel this picker has — `DotHttpErrorManagerService` transitively needs a `Router` the
     * legacy host it runs in does not have (FR-015).
     */
    readonly fieldFilterError = output<DotFilterChipError>();

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

    /**
     * Which content conditions the Status chip may offer, or `null` for no bound.
     *
     * Derived from the caller's version state rather than from any filter, because that is what it
     * is: a property of how the picker was opened. `showWorking === false` is the only value that
     * narrows the request to published content (`live: true`), and {@link PUBLISHED_ONLY_STATUSES}
     * is what may still coexist with it.
     *
     * The same rule is applied to the caller's *seeded* conditions in `buildPickerFilterDefaults`.
     * Both are needed: a seed arrives before any chip exists, and this bound is what keeps the
     * editor from re-adding by hand what the seed was not allowed to carry (SC-009).
     */
    protected readonly $allowedStatuses = computed<DotContentStatus[] | null>(() =>
        this.store.config()?.browse?.showWorking === false ? PUBLISHED_ONLY_STATUSES : null
    );

    /**
     * The field-filter chips on screen, in the order the editor added them.
     *
     * Each variable is resolved against the metadata the "More" overflow published, so a chip
     * renders only once its field is known — the same rule Content Drive applies.
     */
    protected readonly $activeFieldFilters = computed(() => {
        const fieldByVariable = new Map(
            this.store.userSearchableFields().map((field) => [field.variable, field])
        );

        return this.store
            .userSearchableActive()
            .map((variable) => fieldByVariable.get(variable))
            .filter((field): field is DotCMSContentTypeField => field !== undefined);
    });

    /** Folder-pinned upload preference, which drives the button's label. */
    protected readonly $defaultBaseType = computed(() => {
        const data = this.store.selectedNode()?.data;

        return data && data.type !== LOAD_MORE_NODE_TYPE
            ? ((data as TreeNodeContentData).defaultBaseType ?? null)
            : null;
    });
}
