import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnDestroy,
    output,
    viewChild
} from '@angular/core';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentTypeField,
    LOAD_MORE_NODE_TYPE,
    TreeNodeContentData
} from '@dotcms/dotcms-models';

import { DotKeyboardShortcutService } from '../../../../services/dot-keyboard-shortcut/dot-keyboard-shortcut.service';
import { DotKeyboardShortcutUnregister } from '../../../../services/dot-keyboard-shortcut/models';
import { DotContentTypeFilterChipComponent } from '../../../dot-filter-bar/chips/dot-content-type-filter-chip/dot-content-type-filter-chip.component';
import { DotFieldFilterComponent } from '../../../dot-filter-bar/chips/dot-field-filter/dot-field-filter.component';
import { DotFieldFilterMenuComponent } from '../../../dot-filter-bar/chips/dot-field-filter-menu/dot-field-filter-menu.component';
import { DotLanguageFilterChipComponent } from '../../../dot-filter-bar/chips/dot-language-filter-chip/dot-language-filter-chip.component';
import { DotSharedAssetsFilterComponent } from '../../../dot-filter-bar/chips/dot-shared-assets-filter/dot-shared-assets-filter.component';
import { DotContentStatus } from '../../../dot-filter-bar/chips/dot-status-filter/constants';
import { DotStatusFilterComponent } from '../../../dot-filter-bar/chips/dot-status-filter/dot-status-filter.component';
import { DotFilterBarComponent } from '../../../dot-filter-bar/dot-filter-bar.component';
import { DotFilterChipError } from '../../../dot-filter-bar/filter-facade.token';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotUploadButtonComponent } from '../../../dot-upload-button/dot-upload-button.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';
import { allowedStatusesFor } from '../../store/filter-defaults';

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
export class DotAssetPickerToolbarComponent implements OnDestroy {
    readonly store = inject(DotAssetPickerStore);
    readonly #shortcuts = inject(DotKeyboardShortcutService);

    /** Withdrawals for the claims made in the constructor, released in {@link ngOnDestroy}. */
    #withdrawShortcuts: DotKeyboardShortcutUnregister[] = [];

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

    // NOTE: `private`, not `#`, despite TYPESCRIPT_STANDARDS.md:87. Angular's compiler rejects a
    // signal query on an ES-private field: "Cannot use 'viewChild' on a class member that is
    // declared as ES private." The standard cannot be followed here.
    private readonly $searchInput = viewChild.required(DotSearchInputComponent);

    /**
     * Claims the search shortcut while this dialog is open.
     *
     * This is the case the shortcut registry exists for. The picker opens *over* a portlet that has
     * already claimed the same combination, and both search boxes are alive at once. Because claims
     * are per-combination and last-in-wins, registering here takes the key for exactly as long as the
     * dialog is up and hands it straight back on close.
     *
     * Note this claims the *asset* search, not the sidebar's sites-and-folders search: with two boxes
     * on screen the shortcut has to pick one, and the asset list is what the dialog is for.
     */
    constructor() {
        // `viewChild.required`: the search box is rendered unconditionally in this template, so it
        // always resolves by the time a keypress can reach here.
        const focusSearch = () => {
            this.$searchInput().focus();

            return true;
        };

        // Both keys the portlet claims, so the dialog shadows the whole search shortcut rather than
        // half of it — otherwise `/` would still reach the listing behind this dialog. Two calls
        // because labels must be unique within a single `register()` call.
        this.#withdrawShortcuts = ['/', 'mod+k'].map((combination) =>
            this.#shortcuts.register({
                combination,
                label: 'dot.asset.picker.shortcut.search',
                handler: focusSearch
            })
        );
    }

    /**
     * Hands both combinations back to the surface underneath. This is the half that makes the
     * shadowing correct: without it the picker would keep the shortcut after closing, and the
     * portlet behind would lose it for the rest of the session.
     */
    ngOnDestroy(): void {
        this.#withdrawShortcuts.forEach((withdraw) => withdraw());
    }

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
     * A caller restriction, not a filter, so it arrives as an input to the chip rather than through
     * the facade (contract O8a). The rule itself lives in {@link allowedStatusesFor}, which the
     * seeding shares — see there for which bounds exist and why.
     */
    protected readonly $allowedStatuses = computed<DotContentStatus[] | null>(() =>
        allowedStatusesFor(this.store.config())
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
