import { EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { catchError, take } from 'rxjs/operators';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import {
    TITLE_FIELD_VARIABLE,
    USER_SEARCHABLE_FIELD_TYPES,
    USER_SEARCHABLE_PREFIX
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * The "More" filters entry point for the Content Drive toolbar. Enabled only when exactly one
 * content type is selected; it lists that type's User-Searchable + System-Indexed simple fields and
 * lets the user add them as dynamic field-filter chips.
 *
 * It also owns the field-metadata lifecycle: it fetches + caches the selected content type's fields
 * (feeding them to the store for chip rendering and payload reshaping) and clears all active field
 * filters whenever the active content type changes (removed / a second one added / switched to a
 * different single type) — mirroring how the workflow filter reacts to the content-type selection.
 */
@Component({
    selector: 'dot-content-drive-field-filter-menu',
    imports: [
        ButtonModule,
        ListboxModule,
        PopoverModule,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    providers: [DotContentTypeService],
    templateUrl: './dot-content-drive-field-filter-menu.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveFieldFilterMenuComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;

    protected readonly $loading = signal(false);

    /** Monotonic cache of eligible fields per content-type variable, to avoid refetching. */
    readonly #fieldsCache = signal<Record<string, DotCMSContentTypeField[]>>({});

    /** The previously-active single content-type variable, used to detect real changes. */
    #previousActive: string | null = null;
    /** Bumped per fetch so a superseded response can't overwrite newer state. */
    #requestId = 0;

    /** Content-type selection as a stable array (never a freshly minted `[]`). */
    readonly #contentTypes = computed(() => {
        const raw = this.#store.getFilterValue('contentType');

        return Array.isArray(raw) ? raw : undefined;
    });

    /** The single selected content-type variable, or null when 0 or >1 are selected. */
    protected readonly $activeContentType = computed(() => {
        const contentTypes = this.#contentTypes();

        return contentTypes?.length === 1 ? contentTypes[0] : null;
    });

    /** The "More" button is only usable with exactly one content type selected. */
    protected readonly $enabled = computed(() => this.$activeContentType() !== null);

    /** Eligible fields not yet added as chips. */
    protected readonly $availableFields = computed(() => {
        const filters = this.#store.filters();

        return this.#store
            .userSearchableFields()
            .filter((field) => filters[`${USER_SEARCHABLE_PREFIX}${field.variable}`] === undefined);
    });

    constructor() {
        // React to the content-type selection: reset field filters when the active type changes,
        // then (re)load the fields for the new active type.
        effect(() => {
            const active = this.$activeContentType();
            untracked(() => this.#onActiveContentTypeChange(active));
        });
    }

    protected onSelectField(field: DotCMSContentTypeField): void {
        this.#store.addUserSearchableField(field.variable);
    }

    #onActiveContentTypeChange(active: string | null): void {
        if (active === this.#previousActive) {
            return;
        }

        // If we were on a concrete content type and it changed, the previous field filters no
        // longer apply — drop them (and the cached metadata) so nothing stale leaks into the URL.
        if (this.#previousActive !== null) {
            this.#store.clearUserSearchableFilters();
        }

        this.#previousActive = active;

        if (active === null) {
            this.#store.setUserSearchableFields([]);

            return;
        }

        this.#loadFields(active);
    }

    #loadFields(contentTypeVar: string): void {
        const cached = this.#fieldsCache()[contentTypeVar];
        if (cached) {
            this.#store.setUserSearchableFields(cached);

            return;
        }

        const requestId = ++this.#requestId;
        this.$loading.set(true);

        this.#contentTypeService
            .getContentType(contentTypeVar)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    if (requestId === this.#requestId) {
                        this.$loading.set(false);
                        this.#store.setUserSearchableFields([]);
                    }

                    return EMPTY;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((contentType: DotCMSContentType) => {
                if (requestId !== this.#requestId) {
                    return;
                }

                const eligible = this.#eligibleFields(contentType?.fields ?? []);
                this.#fieldsCache.update((cache) => ({ ...cache, [contentTypeVar]: eligible }));
                this.#store.setUserSearchableFields(eligible);
                this.$loading.set(false);
            });
    }

    /**
     * Keeps only fields that are User Searchable + System Indexed and of a simple, supported type.
     * Host-Folder and out-of-scope types are excluded by not being in {@link USER_SEARCHABLE_FIELD_TYPES}.
     * The title field is also excluded — it's already covered by the toolbar's keyword search.
     */
    #eligibleFields(fields: DotCMSContentTypeField[]): DotCMSContentTypeField[] {
        return fields.filter(
            (field) =>
                field.searchable &&
                field.indexed &&
                field.variable?.toLowerCase() !== TITLE_FIELD_VARIABLE &&
                USER_SEARCHABLE_FIELD_TYPES.includes(field.fieldType)
        );
    }
}
