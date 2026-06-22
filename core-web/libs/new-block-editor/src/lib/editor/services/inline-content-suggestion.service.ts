import { Subject, of } from 'rxjs';

import { Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, debounceTime, switchMap } from 'rxjs/operators';

import type { Editor } from '@tiptap/core';

import { DotContentSearchService } from '@dotcms/data-access';
import type { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    buildContentletByTitleQuery,
    type ContentletSearchEntity
} from '../components/slash-menu/slash-menu-catalog';
import { EditorStore } from '../store/editor.store';

/** Debounce window for the live `@`-mention search, in ms. */
const SEARCH_DEBOUNCE_MS = 250;
/** Result cap for the picker. Mirrors the slash-menu contentlet drill-down. */
const SEARCH_LIMIT = 40;

/**
 * Coordinates the inline contentlet `@`-mention picker for a single editor instance: the live
 * title search (debounced), the floating result list, keyboard navigation, and editor focus so
 * the `@tiptap/suggestion` session stays valid when picking from the overlay.
 *
 * Provided at the editor-component scope (next to {@link SlashMenuService}) so multiple editors
 * on one page keep isolated picker state. Search delegates to {@link DotContentSearchService};
 * the query is scoped by the field's `contentTypes` allowlist via {@link EditorStore}.
 */
@Injectable()
export class InlineContentSuggestionService {
    private readonly zone = inject(NgZone);
    private readonly store = inject(EditorStore);
    private readonly contentSearchService = inject(DotContentSearchService);

    /** Contentlet results currently shown in the picker. */
    readonly results = signal<DotCMSContentlet[]>([]);
    /** Whether the floating picker is visible. */
    readonly isOpen = signal(false);
    /** True while a search request is in flight. */
    readonly isLoading = signal(false);
    /** Index of the highlighted row for keyboard navigation. */
    readonly activeIndex = signal(0);
    /** TipTap suggestion anchor: resolves the caret rect for positioning the overlay. */
    readonly clientRectFn = signal<(() => DOMRect | null) | null>(null);
    /** Stable `id` for the active row, or `null` when closed/empty (a11y). */
    readonly activeOptionId = computed(() =>
        this.isOpen() && this.results().length > 0
            ? `inline-content-opt-${this.activeIndex()}`
            : null
    );

    private commandFn: ((contentlet: DotCMSContentlet) => void) | null = null;
    private editor: Editor | null = null;
    private readonly query$ = new Subject<string>();

    constructor() {
        this.query$
            .pipe(
                debounceTime(SEARCH_DEBOUNCE_MS),
                switchMap((query) =>
                    this.contentSearchService
                        .get<ContentletSearchEntity>({
                            query: buildContentletByTitleQuery(
                                query,
                                this.store.languageId(),
                                this.store.allowedContentTypes()
                            ),
                            sort: 'modDate desc',
                            offset: 0,
                            limit: SEARCH_LIMIT
                        })
                        .pipe(catchError(() => of(null)))
                ),
                takeUntilDestroyed()
            )
            .subscribe((entity) => {
                const contentlets = entity?.jsonObjectView?.contentlets ?? [];
                this.zone.run(() => {
                    this.results.set(contentlets);
                    this.activeIndex.set(0);
                    this.isLoading.set(false);
                });
            });
    }

    /** Called by the suggestion extension so UI clicks can refocus the editor before commands run. */
    attachEditor(editor: Editor): void {
        this.editor = editor;
    }

    /** Clears the editor reference when the suggestion plugin is torn down. */
    detachEditor(): void {
        this.editor = null;
    }

    /** Focus the editor before a pointer interaction so the suggestion range isn't lost. */
    prepareMenuPointerInteraction(): void {
        this.editor?.view.focus();
    }

    /**
     * Opens the picker and kicks off the first (empty-query) search.
     *
     * @param query Initial `@`-mention text (usually empty on open).
     * @param clientRectFn Anchor for overlay position; from TipTap suggestion props.
     * @param commandFn Invoked with the chosen contentlet when the user confirms a row.
     */
    open(
        query: string,
        clientRectFn: (() => DOMRect | null) | null,
        commandFn: (contentlet: DotCMSContentlet) => void
    ): void {
        this.commandFn = commandFn;
        this.zone.run(() => {
            this.results.set([]);
            this.clientRectFn.set(clientRectFn);
            this.activeIndex.set(0);
            this.isLoading.set(true);
            this.isOpen.set(true);
        });
        this.query$.next(query);
    }

    /**
     * Refreshes the query and/or anchor while a suggestion session is active.
     *
     * @param query Latest `@`-mention text.
     * @param clientRectFn Updated caret rect.
     * @param commandFn Latest TipTap command callback.
     */
    update(
        query: string,
        clientRectFn: (() => DOMRect | null) | null,
        commandFn: (contentlet: DotCMSContentlet) => void
    ): void {
        this.commandFn = commandFn;
        this.zone.run(() => {
            this.clientRectFn.set(clientRectFn);
            this.isLoading.set(true);
        });
        this.query$.next(query);
    }

    /** Hides the picker and drops TipTap command wiring. */
    close(): void {
        this.zone.run(() => {
            this.isOpen.set(false);
            this.clientRectFn.set(null);
            this.commandFn = null;
            this.isLoading.set(false);
            this.results.set([]);
        });
    }

    /**
     * Confirms a row: refocuses the editor then runs the active command callback.
     * Focusing first avoids losing the `@…` suggestion range when the overlay steals focus.
     */
    select(contentlet: DotCMSContentlet): void {
        this.editor?.view.focus();
        this.commandFn?.(contentlet);
    }

    /**
     * Handles arrow keys, Enter, and Escape while the picker is open.
     *
     * @returns `true` if the event was consumed and should not propagate.
     */
    handleKeyDown(event: KeyboardEvent): boolean {
        if (!this.isOpen()) return false;
        const count = this.results().length;
        switch (event.key) {
            case 'ArrowDown':
                this.zone.run(() => this.activeIndex.update((i) => (i + 1) % Math.max(1, count)));
                return true;
            case 'ArrowUp':
                this.zone.run(() =>
                    this.activeIndex.update(
                        (i) => (i - 1 + Math.max(1, count)) % Math.max(1, count)
                    )
                );
                return true;
            case 'Enter':
                if (count > 0) this.select(this.results()[this.activeIndex()]);
                return true;
            case 'Escape':
                this.close();
                return true;
        }
        return false;
    }
}
