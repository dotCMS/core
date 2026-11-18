import { EMPTY, Observable, of } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    computed,
    contentChild,
    inject,
    input,
    output,
    signal,
    TemplateRef,
    viewChild
} from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { Popover, PopoverModule } from 'primeng/popover';

import { expand, map, reduce } from 'rxjs/operators';

import { DotUserSearchRow, DotUserSearchService, dotUserDisplayName } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import {
    DotLazyMultiselectComponent,
    DotLazyMultiselectItemContext,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption,
    DotLazyMultiselectPage
} from '../dot-filter-bar/chips/dot-field-filter/dot-lazy-multiselect/dot-lazy-multiselect.component';

/** Idle time before a typed term is searched. Matches the listing search boxes it sits beside. */
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Rendered height of the default row, in px: a fixed 36px content box (avatar beside a name and
 * an email line) plus Lara's `0.625rem` top and bottom option padding. The virtual scroller
 * positions rows by this number and cannot measure them.
 */
export const DOT_USER_PICKER_ITEM_HEIGHT = 56;

/**
 * Directory rows fetched per request while filling a page. Larger than the list's own page so
 * that hiding `excludeIds` rarely needs a second request.
 */
const DIRECTORY_FETCH_SIZE = 100;

/**
 * Directory pages read at most per call. Without a ceiling, a role most of the directory already
 * holds would walk the whole directory, one sequential request at a time, before the popover shows
 * anything. Past the ceiling the call returns what it has with `hasMore` still true, and the list
 * asks again — so the walk continues, just in steps the admin can see.
 */
const MAX_FILL_PAGES = 5;

/** Context handed to a custom `#item` template: the directory row being rendered. */
export interface DotUserPickerItemContext {
    $implicit: DotUserSearchRow;
}

/** Where the directory walk stands between one page of the list and the next. */
interface DirectoryCursor {
    /** Next directory page to request (1-based). */
    nextPage: number;
    /** Rows already fetched and not yet handed to the list. */
    buffer: DotUserSearchRow[];
    /** True once the directory has no more rows to give. */
    exhausted: boolean;
}

const freshCursor = (): DirectoryCursor => ({ nextPage: 1, buffer: [], exhausted: false });

/**
 * Popover for choosing people from the dotCMS user directory — the action counterpart of
 * `dot-user-filter`, which narrows a listing instead.
 *
 * The consumer owns the trigger and opens it the way it would a `p-popover`:
 *
 * ```html
 * <p-button label="Add User" (onClick)="picker.toggle($event)" />
 * <dot-user-picker #picker [excludeIds]="memberIds" (picked)="grant($event)" />
 * ```
 *
 * Picks one person by default: the choice is emitted and the popover closes. With `multiple` the
 * list gets checkboxes and an Add button, and everything ticked is emitted together.
 *
 * Paging, virtual scrolling, search debouncing and cancelling a superseded load come from
 * {@link DotLazyMultiselectComponent}; this supplies the directory loader and the row.
 */
@Component({
    selector: 'dot-user-picker',
    imports: [
        NgTemplateOutlet,
        AvatarModule,
        ButtonModule,
        PopoverModule,
        DotLazyMultiselectComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-user-picker.component.html',
    // Not auto-provided — see `DotUserSearchService`. Its lifetime is this picker's.
    providers: [DotUserSearchService]
})
export class DotUserPickerComponent {
    readonly #search = inject(DotUserSearchService);

    /** Whether several people can be chosen at once. Defaults to one. */
    readonly $multiple = input<boolean>(false, { alias: 'multiple' });

    /**
     * People never offered — typically those who already hold what is being granted. Hidden
     * rather than disabled.
     */
    readonly $excludeIds = input<string[]>([], { alias: 'excludeIds' });

    /** Message key for the search box placeholder. */
    readonly $searchPlaceholderKey = input<string>('search', { alias: 'searchPlaceholderKey' });

    /** Message key shown when a search matches nobody. */
    readonly $emptyKey = input<string>('users.filter.empty', { alias: 'emptyKey' });

    /** Message key shown when the directory cannot be read. */
    readonly $errorKey = input<string>('users.filter.error', { alias: 'errorKey' });

    /** Message key for the confirm button shown in multiple mode. */
    readonly $confirmLabelKey = input<string>('add', { alias: 'confirmLabelKey' });

    /**
     * Row height in px for a custom `#item` template. Leave it alone with the default row; change
     * it together with the template, since the scroller cannot measure rows.
     */
    readonly $itemSize = input<number>(DOT_USER_PICKER_ITEM_HEIGHT, { alias: 'itemSize' });

    /** The people chosen: one entry in single mode, every ticked row in multiple mode. */
    readonly picked = output<DotUserSearchRow[]>();

    /**
     * The directory could not be read.
     *
     * Forwarded rather than handled: `@dotcms/ui` cannot reach `DotHttpErrorManagerService` — it
     * transitively needs `Router` and `DotEventsSocket`, absent in the legacy Dojo host. The
     * surface rendering the picker routes it to wherever it reports errors.
     */
    readonly loadFailed = output<HttpErrorResponse>();

    /** Optional row override: `<ng-template #item let-user>…</ng-template>`. */
    protected readonly $customItem = contentChild<TemplateRef<DotUserPickerItemContext>>('item');

    protected readonly popover = viewChild.required(Popover);
    protected readonly DEBOUNCE_MS = SEARCH_DEBOUNCE_MS;

    /** Rows ticked so far in multiple mode, waiting for the Add button. */
    protected readonly $pending = signal<DotUserSearchRow[]>([]);

    readonly #excluded = computed(() => new Set(this.$excludeIds()));

    #cursor = freshCursor();

    /**
     * Loads one page of people for the list, with `excludeIds` already taken out.
     *
     * Hiding rows is why this is more than a mapped request. The list only asks for another page
     * while each one comes back full — a short page reads as the end — so a page thinned by
     * exclusions would stop the scroll. Each call therefore keeps reading the directory until it
     * can hand back a full page or the directory runs out, and carries any surplus to the next
     * call.
     *
     * Page 1 means a new search (or a fresh open) and starts the walk over. The cursor is only
     * committed once a call completes, so a load the list cancels leaves it untouched.
     */
    readonly loadPage: DotLazyMultiselectLoader = ({ page, perPage, filter }) => {
        const start: DirectoryCursor = page === 1 ? freshCursor() : { ...this.#cursor };

        return this.#fill(start, perPage, filter).pipe(
            map((cursor) => {
                const rows = cursor.buffer.slice(0, perPage);
                this.#cursor = { ...cursor, buffer: cursor.buffer.slice(perPage) };

                return {
                    options: rows.map((row) => ({
                        // The id is the value because that is what a consumer acts on; the name
                        // is only ever displayed.
                        value: row.userId,
                        label: dotUserDisplayName(row),
                        data: row
                    })),
                    hasMore: this.#cursor.buffer.length > 0 || !this.#cursor.exhausted
                } satisfies DotLazyMultiselectPage;
            })
        );
    };

    /** Opens or closes the picker, anchored to the event's target. */
    toggle(event: Event): void {
        this.popover().toggle(event);
    }

    /** Closes the picker. */
    hide(): void {
        this.popover().hide();
    }

    /**
     * Every open starts with nothing ticked. The directory walk needs no reset here: the list is
     * created on open and asks for page 1, which starts it over — and resetting here too could
     * discard a cursor that page already committed, since PrimeNG emits `onShow` later.
     */
    protected onShow(): void {
        this.$pending.set([]);
    }

    protected onSelectionChange(options: DotLazyMultiselectOption[]): void {
        const rows = options
            .map((option) => option.data as DotUserSearchRow | undefined)
            .filter((row): row is DotUserSearchRow => !!row);

        if (this.$multiple()) {
            this.$pending.set(rows);

            return;
        }

        if (rows.length) {
            this.picked.emit(rows);
            this.hide();
        }
    }

    protected onConfirm(): void {
        const rows = this.$pending();
        if (!rows.length) {
            return;
        }

        this.picked.emit(rows);
        this.hide();
    }

    /** Initials for the default avatar: first + last name, else the email's first letter. */
    protected initialsFor(row: DotUserSearchRow): string {
        const first = row.firstName?.trim()?.[0] ?? '';
        const last = row.lastName?.trim()?.[0] ?? '';
        const initials = `${first}${last}`.toUpperCase();

        return initials || row.emailAddress?.trim()?.[0]?.toUpperCase() || '?';
    }

    /** Casts the list's option context back to the directory row the default row renders. */
    protected rowOf(context: DotLazyMultiselectItemContext['$implicit']): DotUserSearchRow {
        return context.data as DotUserSearchRow;
    }

    /**
     * Reads directory pages into the buffer until it holds `perPage` visible rows, the directory
     * is exhausted, or {@link MAX_FILL_PAGES} pages have been read in this call.
     */
    #fill(start: DirectoryCursor, perPage: number, filter: string): Observable<DirectoryCursor> {
        const lastPage = start.nextPage + MAX_FILL_PAGES - 1;

        const step = (cursor: DirectoryCursor): Observable<DirectoryCursor> => {
            if (cursor.buffer.length >= perPage || cursor.exhausted || cursor.nextPage > lastPage) {
                return EMPTY;
            }

            return this.#search
                .searchPage({ filter, page: cursor.nextPage, perPage: DIRECTORY_FETCH_SIZE })
                .pipe(
                    map((response) => {
                        const rows = response?.entity ?? [];
                        // A missing total must not read as "no more": `?? 0` would end the walk
                        // after one page and silently truncate the list. An empty page still
                        // ends it when the directory genuinely runs out.
                        const total = response?.pagination?.totalEntries ?? Infinity;
                        const excluded = this.#excluded();

                        return {
                            nextPage: cursor.nextPage + 1,
                            buffer: [
                                ...cursor.buffer,
                                ...rows.filter((row) => !excluded.has(row.userId))
                            ],
                            exhausted:
                                rows.length === 0 || cursor.nextPage * DIRECTORY_FETCH_SIZE >= total
                        };
                    })
                );
        };

        return of(start).pipe(
            expand(step),
            reduce((_, cursor) => cursor, start)
        );
    }
}
