import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    output,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { CHIP_FILTER_LISTBOX_PT, CHIP_FILTER_POPOVER_PT } from '../../../dot-chip-filter/constants';
import { DotFilterListItemComponent } from '../../../dot-filter-list-item/dot-filter-list-item.component';
import { DotFilterChipError, DOT_FILTER_FACADE, toFilterValues } from '../../filter-facade.token';
import { TITLE_FIELD_VARIABLE, USER_SEARCHABLE_FIELD_TYPES } from '../dot-field-filter/constants';
import { DOT_FIELD_FILTER_HOST } from '../dot-field-filter/field-filter-host.token';

/** Translation key the surface shows when the field fetch fails (FR-015). */
const FIELD_FETCH_ERROR_KEY = 'content-drive.field-filter.more.error';

/**
 * The "More" filters entry point, shared by every surface that offers field filters.
 *
 * Enabled only when exactly one content type is selected; it lists that type's User-Searchable +
 * System-Indexed simple fields and lets the editor add them as dynamic field-filter chips.
 *
 * It also owns the field-metadata lifecycle: it fetches and caches the selected content type's
 * fields — publishing them through {@link DOT_FIELD_FILTER_HOST} so the chips can render controls
 * and the surface's request builder can reshape values — and clears all active field filters
 * whenever the active content type changes (removed / a second one added / switched to a different
 * single type), mirroring how the workflow filter reacts to the content-type selection.
 *
 * **Errors leave through `error`, not a service.** `DotHttpErrorManagerService` transitively needs
 * `Router` and `DotEventsSocket`, which the legacy Dojo host `@dotcms/ui` is bundled into does not
 * have. Content Drive routes the output to that service as before; the AssetPicker routes it to its
 * own in-dialog toast (contract §3).
 */
@Component({
    selector: 'dot-field-filter-menu',
    imports: [
        ButtonModule,
        ListboxModule,
        PopoverModule,
        TooltipModule,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    providers: [DotContentTypeService],
    templateUrl: './dot-field-filter-menu.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // The "More" menu anchors the `fieldFilters` slot in the canonical order, not the dynamic chips
    // it mints: the order check walks ids as a subsequence, so two elements carrying the same id
    // would fail it the moment a field filter is active.
    host: { 'data-filter-chip': 'fieldFilters' }
})
export class DotFieldFilterMenuComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);
    readonly #host = inject(DOT_FIELD_FILTER_HOST);
    readonly #destroyRef = inject(DestroyRef);
    readonly #contentTypeService = inject(DotContentTypeService);

    /**
     * Reported when the field fetch fails, so the surface announces it its own way.
     *
     * The chip's job is to say *that* the options could not load; how that reaches the user is the
     * surface's call, and the two surfaces genuinely differ (contract §3).
     */
    readonly error = output<DotFilterChipError>();

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;

    protected readonly $loading = signal(false);

    /** Monotonic cache of the raw fields per content-type variable, to avoid refetching. */
    readonly #fieldsCache = signal<Record<string, DotCMSContentTypeField[]>>({});

    /** The previously-active single content-type variable, used to detect real changes. */
    #previousActive: string | null = null;
    /** Emits the content-type variable to load; switchMap cancels any superseded fetch. */
    readonly #loadFields$ = new Subject<string>();

    /** The single selected content-type variable, or null when 0 or >1 are selected. */
    protected readonly $activeContentType = computed(() => {
        const contentTypes = toFilterValues(this.#filters.getFilterValue('contentType'));

        return contentTypes.length === 1 ? contentTypes[0] : null;
    });

    /** The "More" button is only usable with exactly one content type selected. */
    protected readonly $enabled = computed(() => this.$activeContentType() !== null);

    /** Eligible fields not yet added as chips (added ones are already on screen). */
    protected readonly $availableFields = computed(() => {
        const active = this.#host.$activeFields();

        return this.#host.$fields().filter((field) => !active.includes(field.variable));
    });

    constructor() {
        // Fetch the active content type's fields. switchMap cancels a superseded fetch when the
        // content type changes again mid-flight; cached types resolve synchronously.
        this.#loadFields$
            .pipe(
                tap(() => this.$loading.set(true)),
                switchMap((contentTypeVar) => {
                    const cached = this.#fieldsCache()[contentTypeVar];
                    if (cached) {
                        return of(cached);
                    }

                    return this.#contentTypeService.getContentType(contentTypeVar).pipe(
                        take(1),
                        map((contentType: DotCMSContentType) => {
                            const raw = contentType?.fields ?? [];
                            this.#fieldsCache.update((cache) => ({
                                ...cache,
                                [contentTypeVar]: raw
                            }));

                            return raw;
                        }),
                        catchError(() => {
                            // Degrade to an empty option list and stay interactive: the surface
                            // decides how to announce this, and neither surface may be taken down
                            // by a filter's options failing to load (FR-015).
                            this.error.emit({ messageKey: FIELD_FETCH_ERROR_KEY });

                            return of<DotCMSContentTypeField[]>([]);
                        })
                    );
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((fields) => {
                // One fetch feeds three consumers: this menu (which fields are still addable), the
                // field-filter chips (which control each renders), and the surface's request
                // builder (which reshapes a raw value into the `userSearchable` payload). Content
                // Drive additionally mines the raw list for its table's "Show In List" columns,
                // which is why both lists cross the seam.
                this.#host.setFields({ eligible: this.#eligibleFields(fields), all: fields });
                this.$loading.set(false);
            });

        // React to the content-type selection: reset field filters when the active type changes,
        // then (re)load the fields for the new active type.
        effect(() => {
            const active = this.$activeContentType();
            untracked(() => this.#onActiveContentTypeChange(active));
        });
    }

    protected onSelectField(field: DotCMSContentTypeField): void {
        this.#host.addField(field.variable);
    }

    #onActiveContentTypeChange(active: string | null): void {
        if (active === this.#previousActive) {
            return;
        }

        // If we were on a concrete content type and it changed, the previous field filters no
        // longer apply — drop them (and the cached metadata) so nothing stale leaks into the
        // request or the URL.
        if (this.#previousActive !== null) {
            this.#host.clearFields();
        }

        this.#previousActive = active;

        if (active === null) {
            this.#host.setFields({ eligible: [], all: [] });

            return;
        }

        this.#loadFields$.next(active);
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
