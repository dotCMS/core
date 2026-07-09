import { Injectable, NgZone, computed, inject, signal } from '@angular/core';

import type { Editor } from '@tiptap/core';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import type { Action } from '@dotcms/dotcms-models';

import {
    createBaseBlockItems,
    createContentTypeItem,
    createSlashAiBlockItems,
    createSlashOverlayBlockItems,
    createSlashRemoteBlockItems
} from './slash-menu-catalog';

import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';

import type { BlockItem } from './slash-menu.types';

export type { BlockItem } from './slash-menu.types';
export { createBaseBlockItems } from './slash-menu-catalog';

/** Debounce window for async sub-menu server searches (content types / contentlets), in ms. */
const SUBMENU_SEARCH_DEBOUNCE_MS = 250;

/**
 * Coordinates the TipTap slash-command floating menu: item catalog, filtering,
 * sub-menu loading for content types, keyboard navigation, and editor focus
 * so the suggestion session stays valid when picking from the overlay.
 */
@Injectable()
export class SlashMenuService {
    private readonly zone = inject(NgZone);
    private readonly store = inject(EditorStore);
    private readonly popovers = inject(EditorPopoverService);
    private readonly editorModal = inject(EditorModalService);
    private readonly contentTypeService = inject(DotContentTypeService);
    private readonly contentSearchService = inject(DotContentSearchService);
    private readonly dotMessageService = inject(DotMessageService);

    private readonly baseBlockItems = createBaseBlockItems(this.dotMessageService);
    private readonly overlayBlockItems = createSlashOverlayBlockItems(
        this.popovers,
        this.editorModal,
        this.dotMessageService
    );
    private readonly aiBlockItems = createSlashAiBlockItems(
        this.editorModal,
        this.dotMessageService
    );
    private remoteBlockItems: BlockItem[] = [];

    /**
     * Replaces the customer-supplied remote actions shown at the end of the slash menu.
     * Called by the editor component once {@link loadRemoteExtensions} resolves, so the
     * slash menu picks them up on the next `filterItems` pass without any re-open.
     */
    setRemoteBlockItems(actions: Action[]): void {
        this.remoteBlockItems = createSlashRemoteBlockItems(actions);
    }

    private readonly contentTypeItem = createContentTypeItem(
        this,
        this.contentTypeService,
        this.contentSearchService,
        () => this.store.languageId(),
        () => this.store.allowedContentTypes(),
        this.dotMessageService
    );

    /**
     * Returns menu items for the text after `/`, respecting allowed block types from {@link EditorStore}.
     * While a sub-menu is open, filters the cached sub-menu list instead of the root catalog.
     *
     * @param query Text after the slash; matched case-insensitively against labels and keywords.
     */
    filterItems(query: string): BlockItem[] {
        if (this.isInSubmenu) {
            if (this.submenuSearch) {
                // Server-side search: re-query (debounced) as the user types. Return the current
                // rows synchronously so TipTap's onUpdate keeps them on screen until the async
                // results land via runSubmenuSearch().
                this.scheduleSubmenuSearch(query);
                return this.items();
            }
            const q = query.toLowerCase().trim();
            if (!q) return this.subMenuAllItems;
            return this.subMenuAllItems.filter(
                (item) =>
                    item.label.toLowerCase().includes(q) || item.keywords.some((k) => k.includes(q))
            );
        }

        const aiItems = this.store.aiInstalled() === true ? this.aiBlockItems : [];
        const all = [
            this.contentTypeItem,
            ...this.baseBlockItems,
            ...this.overlayBlockItems,
            ...aiItems,
            ...this.remoteBlockItems
        ];

        const filtered = all.filter(
            (item) =>
                !item.blockName ||
                item.blockName === 'paragraph' ||
                this.store.isAllowed(item.blockName)
        );

        const q = query.toLowerCase().trim();
        if (!q) return filtered;
        return filtered.filter(
            (item) =>
                item.label.toLowerCase().includes(q) || item.keywords.some((k) => k.includes(q))
        );
    }

    /** Options currently shown in the slash menu (root or sub-menu). */
    readonly items = signal<BlockItem[]>([]);
    /** Whether the floating menu is visible. */
    readonly isOpen = signal(false);
    /** True while async sub-menu items (e.g. content types) are loading. */
    readonly isLoading = signal(false);
    /** Index of the highlighted row for keyboard navigation. */
    readonly activeIndex = signal(0);
    /** TipTap suggestion anchor: resolves the caret rect for positioning the overlay. */
    readonly clientRectFn = signal<(() => DOMRect | null) | null>(null);
    /** Stable `id` for the active row, or `null` when the menu is closed or empty (a11y). */
    readonly activeOptionId = computed(() =>
        this.isOpen() && this.items().length > 0 ? `slash-opt-${this.activeIndex()}` : null
    );

    private commandFn: ((item: BlockItem) => void) | null = null;
    private editor: Editor | null = null;
    private isInSubmenu = false;
    /**
     * Full unfiltered sub-menu list while the content-type (or similar) sub-menu is open.
     * Kept separately from {@link items} so {@link filterItems} can re-filter as the user types.
     */
    private subMenuAllItems: BlockItem[] = [];

    /**
     * When set, the current sub-menu searches the SERVER on each keystroke (debounced) instead of
     * client-filtering {@link subMenuAllItems}. Required for the content-type / contentlet pickers:
     * the server may hold thousands of records, so a fixed top-N fetch + client filter silently
     * hides anything past the cap (see the "list ends after Image Asset" report). Mirrors the
     * legacy editor's re-query-on-type behaviour.
     */
    private submenuSearch: ((query: string) => Promise<BlockItem[]>) | null = null;
    private submenuSearchTimer: ReturnType<typeof setTimeout> | null = null;
    /** Bumped on every kicked-off search so a late/stale response can't overwrite newer results. */
    private submenuSearchToken = 0;
    /** Last query a search was started for — dedupes repeated identical queries (e.g. the empty query on open). */
    private submenuPendingQuery: string | null = null;

    /**
     * Called by the slash-command extension so UI interactions can refocus the editor before running commands.
     *
     * @param editor Active TipTap editor instance for this menu.
     */
    attachEditor(editor: Editor): void {
        this.editor = editor;
    }

    /** Clears the editor reference when the slash suggestion plugin is torn down. */
    detachEditor(): void {
        this.editor = null;
    }

    /**
     * Call from `pointerdown` capture on the menu so the editor is focused before the event target runs.
     */
    prepareMenuPointerInteraction(): void {
        this.editor?.view.focus();
    }

    /**
     * Opens the slash menu with an initial item list and TipTap suggestion wiring.
     *
     * @param items Rows to display immediately.
     * @param clientRectFn Anchor for overlay position; from TipTap suggestion props.
     * @param commandFn Invoked when the user confirms a row (Enter / click).
     */
    open(
        items: BlockItem[],
        clientRectFn: (() => DOMRect | null) | null,
        commandFn: (item: BlockItem) => void
    ): void {
        this.zone.run(() => {
            this.items.set(items);
            this.clientRectFn.set(clientRectFn);
            this.commandFn = commandFn;
            this.activeIndex.set(0);
            this.isOpen.set(true);
        });
    }

    /**
     * Refreshes the visible rows and/or anchor while a suggestion session is active.
     * In a sub-menu, only updates {@link items} and the active index — preserves {@link commandFn}
     * because TipTap's callback would otherwise call `deleteRange` on the slash trigger.
     *
     * @param items Latest filtered list (from {@link filterItems}).
     * @param clientRectFn Updated caret rect, ignored while in a sub-menu.
     * @param commandFn Latest TipTap command callback, ignored while in a sub-menu.
     */
    update(
        items: BlockItem[],
        clientRectFn: (() => DOMRect | null) | null,
        commandFn: (item: BlockItem) => void
    ): void {
        if (this.isInSubmenu) {
            this.zone.run(() => {
                this.items.set(items);
                this.activeIndex.set(0);
            });
            return;
        }
        this.zone.run(() => {
            this.items.set(items);
            this.clientRectFn.set(clientRectFn);
            this.commandFn = commandFn;
            this.activeIndex.set(0);
        });
    }

    /** Hides the menu, clears sub-menu state, and drops TipTap command wiring. */
    close(): void {
        this.isInSubmenu = false;
        this.subMenuAllItems = [];
        this.submenuSearch = null;
        this.submenuPendingQuery = null;
        if (this.submenuSearchTimer) {
            clearTimeout(this.submenuSearchTimer);
            this.submenuSearchTimer = null;
        }
        // Invalidate any in-flight search so it can't repopulate a closed menu.
        this.submenuSearchToken++;
        this.zone.run(() => {
            this.isOpen.set(false);
            this.clientRectFn.set(null);
            this.commandFn = null;
            this.isLoading.set(false);
        });
    }

    /**
     * Opens a sub-menu whose rows come from a debounced SERVER search that re-runs on every
     * keystroke — used by the content-type and contentlet pickers so results aren't capped by a
     * one-shot top-N fetch. The initial (empty-query) search runs immediately; subsequent queries
     * are debounced. `search` must always resolve (map its own errors to empty/error-state rows);
     * a rejection just clears the loading state.
     *
     * @param search Resolves the rows for a given query.
     * @param commandFn Handler for picks in this sub-menu (preserved across re-queries).
     */
    openAsyncSubmenu(
        search: (query: string) => Promise<BlockItem[]>,
        commandFn: (item: BlockItem) => void
    ): void {
        this.isInSubmenu = true;
        this.submenuSearch = search;
        this.subMenuAllItems = [];
        this.submenuPendingQuery = '';
        if (this.submenuSearchTimer) {
            clearTimeout(this.submenuSearchTimer);
            this.submenuSearchTimer = null;
        }
        this.zone.run(() => {
            this.items.set([]);
            this.activeIndex.set(0);
            this.isLoading.set(true);
            this.commandFn = commandFn;
            // isOpen and clientRectFn unchanged — menu is already visible and positioned
        });
        this.runSubmenuSearch('', ++this.submenuSearchToken);
    }

    /** Debounced re-query for the active async sub-menu; dedupes consecutive identical queries. */
    private scheduleSubmenuSearch(query: string): void {
        if (query === this.submenuPendingQuery) return;
        this.submenuPendingQuery = query;
        if (this.submenuSearchTimer) clearTimeout(this.submenuSearchTimer);
        this.zone.run(() => this.isLoading.set(true));
        const token = ++this.submenuSearchToken;
        this.submenuSearchTimer = setTimeout(
            () => this.runSubmenuSearch(query, token),
            SUBMENU_SEARCH_DEBOUNCE_MS
        );
    }

    /** Runs the active async sub-menu search and applies the result unless it's stale. */
    private runSubmenuSearch(query: string, token: number): void {
        const search = this.submenuSearch;
        if (!search) return;
        search(query)
            .then((items) => {
                // Drop results that arrive after a newer query, or after the sub-menu closed /
                // switched level, so a slow response can't overwrite the current rows.
                if (token !== this.submenuSearchToken || !this.isInSubmenu) return;
                this.subMenuAllItems = items;
                this.zone.run(() => {
                    this.items.set(items);
                    this.activeIndex.set(0);
                    this.isLoading.set(false);
                });
            })
            .catch(() => {
                if (token !== this.submenuSearchToken || !this.isInSubmenu) return;
                this.zone.run(() => this.isLoading.set(false));
            });
    }

    /**
     * Confirms a menu row: refocuses the editor then runs the active command callback.
     *
     * Focusing first avoids losing the `/…` suggestion range when the overlay steals focus,
     * which would exit `@tiptap/suggestion` and call {@link close} before the command runs.
     *
     * @param item Row the user chose.
     */
    select(item: BlockItem): void {
        this.editor?.view.focus();
        this.commandFn?.(item);
    }

    /**
     * Handles arrow keys, Enter, and Escape while the menu is open.
     *
     * @param event Native keyboard event from the editor host.
     * @returns `true` if the event was consumed and should not propagate.
     */
    handleKeyDown(event: KeyboardEvent): boolean {
        if (!this.isOpen()) return false;
        const count = this.items().length;
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
                if (count > 0) this.select(this.items()[this.activeIndex()]);
                return true;
            case 'Escape':
                this.close();
                return true;
        }
        return false;
    }
}
