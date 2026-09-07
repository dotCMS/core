import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    output,
    viewChild
} from '@angular/core';

import {
    DotCMSBaseTypesContentTypes,
    LOAD_MORE_NODE_TYPE,
    TreeNodeContentData
} from '@dotcms/dotcms-models';

import { DotKeyboardShortcutService } from '../../../../services/dot-keyboard-shortcut/dot-keyboard-shortcut.service';
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
    readonly #shortcuts = inject(DotKeyboardShortcutService);

    /** Re-emitted to the shell, which owns the upload flow. */
    readonly upload = output<MouseEvent>();

    private readonly $searchInput = viewChild(DotSearchInputComponent);

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
        const unregister = this.#shortcuts.register({
            combination: 'mod+k',
            label: 'dot.asset.picker.shortcut.search',
            handler: () => {
                const input = this.$searchInput();

                // Declines rather than swallowing the key if the box is not rendered, so the
                // combination falls through to the portlet underneath instead of doing nothing.
                if (!input) {
                    return false;
                }

                input.focus();

                return true;
            }
        });

        inject(DestroyRef).onDestroy(unregister);
    }

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
