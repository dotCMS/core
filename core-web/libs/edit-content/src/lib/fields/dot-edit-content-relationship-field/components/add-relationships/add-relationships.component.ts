import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    OnInit,
    Renderer2,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';


import { DotCMSContentlet, DotContentDriveBrowseItem } from '@dotcms/dotcms-models';
import {
    applyDialogFullscreen,
    prefersReducedMotion,
    DotDialogComponent,
    DotDialogContentComponent,
    DotDialogFooterComponent,
    DotDialogHeaderComponent,
    DotFilterBarComponent,
    DotFolderListViewComponent,
    DotLanguageFilterChipComponent,
    DotMessagePipe,
    DotSearchInputComponent
} from '@dotcms/ui';

import { AddRelationshipsFooterComponent } from './components/footer/footer.component';
import { AddRelationshipsSiteChipComponent } from './components/site-chip/add-relationships-site-chip.component';
import { AddRelationshipsInput, AddRelationshipsResult } from './models/add-relationships.models';
import { provideAddRelationshipsFilterFacade } from './store/add-relationships-filter-facade';
import { AddRelationshipsStore } from './store/add-relationships.store';

/**
 * The "Add Relationships" dialog.
 *
 * Composes the shared search surface — `dot-search-input`, `dot-filter-bar` and
 * `dot-folder-list-view` — rather than reusing the AssetPicker shell, because the design has no
 * folder-tree sidebar: content of an arbitrary content type is not folder-scoped, so the folder
 * scope is a chip.
 *
 * It lives in `@dotcms/edit-content` and not in `@dotcms/ui`, and that is not a preference. The
 * shared library is compiled into the legacy custom-element bundle and `edit-content` already
 * depends on it in ~98 files, so importing this from there would be circular *and* would drag the
 * whole feature library into that bundle. The one place shared code needs to open this dialog —
 * Content Drive's field-filter chip — inverts the dependency through `DOT_RELATIONSHIP_PICKER`
 * instead.
 *
 * **Two consumers, and they are not symmetric.** See
 * `specs/37192-relationship-field-assetpicker/contracts/relationship-picker.contract.md`.
 */
@Component({
    selector: 'dot-add-relationships',
    imports: [
        ButtonModule,
        TooltipModule,
        FormsModule,
        ToggleSwitchModule,
        DotDialogComponent,
        DotDialogHeaderComponent,
        DotDialogContentComponent,
        DotDialogFooterComponent,
        DotFilterBarComponent,
        DotFolderListViewComponent,
        DotLanguageFilterChipComponent,
        DotSearchInputComponent,
        DotMessagePipe,
        AddRelationshipsSiteChipComponent,
        AddRelationshipsFooterComponent
    ],
    templateUrl: './add-relationships.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [AddRelationshipsStore, provideAddRelationshipsFilterFacade()]
})
export class AddRelationshipsComponent implements OnInit {
    protected readonly store = inject(AddRelationshipsStore);

    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig =
        inject<DynamicDialogConfig<AddRelationshipsInput>>(DynamicDialogConfig);

    /**
     * Whether the dialog is expanded.
     *
     * The flag is all this component owns; **resizing the host `.p-dialog` is separate work** and
     * was the missing half — the toggle flipped a signal nothing reacted to, so the button did
     * nothing at all. Same split the AssetPicker uses, through the same shared helper.
     */
    readonly #isFullscreen = signal(false);

    /** PrimeNG's dialog, when this component is rendered inside one. */
    readonly #dialog = inject(Dialog, { optional: true });
    readonly #renderer = inject(Renderer2);
    readonly #document = inject(DOCUMENT);

    protected readonly $selectionMode = computed(
        () => this.#dialogConfig.data?.selectionMode ?? 'multiple'
    );

    protected readonly $searchTerm = computed(() => {
        const title = this.store.getFilterValue('title');

        return Array.isArray(title) ? (title[0] ?? '') : (title ?? '');
    });

    /** Material Symbol ligature, matching the Asset Picker and the Image Editor. */
    protected readonly $fullscreenIcon = computed(() =>
        this.#isFullscreen() ? 'close_fullscreen' : 'open_in_full'
    );

    protected readonly $isFullscreen = this.#isFullscreen.asReadonly();

    protected readonly $fullscreenLabel = computed(() =>
        this.#isFullscreen()
            ? 'dot.relationship.add.dialog.fullscreen.exit.aria'
            : 'dot.relationship.add.dialog.fullscreen.enter.aria'
    );

    constructor() {
        // The dialog owns its own header, so it owns full screen too: resize the host `.p-dialog`
        // whenever the flag flips. Requires `maximizable: true` in the dialog config — without it
        // PrimeNG ignores `maximize()` and the class alone fights the inline size.
        effect(() =>
            applyDialogFullscreen({
                dialog: this.#dialog,
                renderer: this.#renderer,
                on: this.#isFullscreen(),
                prefersReducedMotion: prefersReducedMotion(this.#document)
            })
        );
    }

    ngOnInit(): void {
        const input = this.#dialogConfig.data;

        if (!input) {
            return;
        }

        this.store.initialize(input);
        this.store.load();
    }

    protected toggleFullscreen(): void {
        this.#isFullscreen.update((value) => !value);
    }

    protected onSearch(term: string): void {
        if (term) {
            this.store.patchFilters({ title: term });
        } else {
            this.store.removeFilter('title');
        }

        this.store.load();
    }

    /**
     * The shared list reports what is checked **on the current page**. The store reconciles that
     * against the accumulated selection, so picks made on another page — or seeded by the caller
     * and never listed at all — survive.
     */
    protected onSelectionChange(items: DotContentDriveBrowseItem[]): void {
        const checked = new Set(items.map((item) => item.identifier));

        for (const item of this.store.items()) {
            const isChecked = checked.has(item.identifier);

            if (isChecked !== this.store.$isSelected()(item.identifier)) {
                this.store.toggleSelection(item as DotCMSContentlet);
            }
        }
    }

    /**
     * Switches between the page of results and the editor's own selection.
     *
     * No reload: the selected view reads from the accumulated selection rather than from a search,
     * which is the whole reason it can show picks the current page does not contain.
     */
    protected onViewModeChange(showSelected: boolean): void {
        this.store.setViewMode(showSelected ? 'selected' : 'all');
    }

    /**
     * The empty state's way out: back to how the dialog opened — filters **and** scope.
     *
     * Not just `clearFilters`: an editor who browsed into a scope with nothing in it has no filters
     * to clear, and clearing only filters would leave them exactly where they were stuck.
     */
    protected onClearFilters(): void {
        this.store.reset();
    }

    /**
     * Moves to the page the footer asked for, carrying its page size.
     *
     * The size travels because the shared list offers 20/40/60 and the store has to fetch what the
     * table is about to render — dropping it left the two disagreeing about how big a page is.
     * Leaves the selection alone, like every browse path.
     */
    protected onPaginate(event: { first?: number; rows?: number; page?: number }): void {
        // Both can legitimately be 0, so check presence rather than truthiness.
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.store.setPage((event.page ?? 0) + 1, event.rows);
        this.store.load();
    }

    protected onSort(event: { field?: string; order?: number }): void {
        this.store.setSort({
            field: event.field ?? 'modDate',
            order: event.order === 1 ? 'asc' : 'desc'
        });
    }

    /**
     * Closes with the editor's whole selection.
     *
     * An **empty array is a real result** — it means they unchecked everything, and the
     * relationship is emptied. That is why the confirm button carries no `disabled` binding.
     */
    protected confirm(): void {
        const result: AddRelationshipsResult = this.store.$selectedItems() as DotCMSContentlet[];
        this.#dialogRef.close(result);
    }

    /**
     * Closes with `undefined`, which the caller must not confuse with a confirmed empty selection:
     * one leaves the relationship untouched, the other empties it.
     */
    protected cancel(): void {
        this.#dialogRef.close(undefined);
    }
}
