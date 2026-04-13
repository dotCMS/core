import { Injectable, NgZone, computed, inject, signal } from '@angular/core';

import type { Editor } from '@tiptap/core';

import {
    ALL_ITEMS,
    createContentTypeItem,
    createSlashDialogBlockItems
} from './slash-menu-catalog';

import { ImageDialogService } from '../components/image/image-dialog.service';
import { TableDialogService } from '../components/table/table-dialog.service';
import { VideoDialogService } from '../components/video/video-dialog.service';
import { DotCmsContentTypeService } from '../services/dot-cms-content-type.service';
import { DotCmsContentletService } from '../services/dot-cms-contentlet.service';

import type { BlockItem } from './slash-menu.types';

export type { BlockItem } from './slash-menu.types';
export { ALL_ITEMS } from './slash-menu-catalog';

@Injectable({ providedIn: 'root' })
export class SlashMenuService {
    private readonly zone = inject(NgZone);
    private readonly tableDialogService = inject(TableDialogService);
    private readonly imageDialogService = inject(ImageDialogService);
    private readonly videoDialogService = inject(VideoDialogService);
    private readonly contentTypeService = inject(DotCmsContentTypeService);
    private readonly contentletService = inject(DotCmsContentletService);

    private readonly dialogBlockItems = createSlashDialogBlockItems({
        table: this.tableDialogService,
        image: this.imageDialogService,
        video: this.videoDialogService
    });

    private readonly contentTypeItem = createContentTypeItem(
        this,
        this.contentTypeService,
        this.contentletService
    );

    readonly allowedBlocks = signal<string[] | null>(null);

    filterItems(query: string): BlockItem[] {
        // While in a sub-menu, filter the content type list instead of the regular items.
        if (this.isInSubmenu) {
            const q = query.toLowerCase().trim();
            if (!q) return this.subMenuAllItems;
            return this.subMenuAllItems.filter(
                (item) =>
                    item.label.toLowerCase().includes(q) || item.keywords.some((k) => k.includes(q))
            );
        }

        const all = [this.contentTypeItem, ...ALL_ITEMS, ...this.dialogBlockItems];

        const allowed = this.allowedBlocks();
        const filtered = allowed
            ? all.filter(
                  (item) =>
                      !item.blockName ||
                      item.blockName === 'paragraph' ||
                      allowed.includes(item.blockName)
              )
            : all;

        const q = query.toLowerCase().trim();
        if (!q) return filtered;
        return filtered.filter(
            (item) =>
                item.label.toLowerCase().includes(q) || item.keywords.some((k) => k.includes(q))
        );
    }

    readonly items = signal<BlockItem[]>([]);
    readonly isOpen = signal(false);
    readonly isLoading = signal(false);
    readonly activeIndex = signal(0);
    readonly clientRectFn = signal<(() => DOMRect | null) | null>(null);
    readonly activeOptionId = computed(() =>
        this.isOpen() && this.items().length > 0 ? `slash-opt-${this.activeIndex()}` : null
    );

    private commandFn: ((item: BlockItem) => void) | null = null;
    private editor: Editor | null = null;
    private isInSubmenu = false;
    // Full unfiltered sub-menu list — kept separate so filterItems() can re-filter it
    // as the user types while the sub-menu is open.
    private subMenuAllItems: BlockItem[] = [];

    /** Set by slash-command extension so menu clicks can re-focus the editor before selection runs. */
    attachEditor(editor: Editor): void {
        this.editor = editor;
    }

    detachEditor(): void {
        this.editor = null;
    }

    /** Call from pointerdown capture on the menu so focus returns before the target runs. */
    prepareMenuPointerInteraction(): void {
        this.editor?.view.focus();
    }

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

    update(
        items: BlockItem[],
        clientRectFn: (() => DOMRect | null) | null,
        commandFn: (item: BlockItem) => void
    ): void {
        if (this.isInSubmenu) {
            // items is already the result of filterItems() — apply it but keep our commandFn
            // (Tiptap's commandFn would wrongly call deleteRange on the slash trigger).
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

    close(): void {
        this.isInSubmenu = false;
        this.subMenuAllItems = [];
        this.zone.run(() => {
            this.isOpen.set(false);
            this.clientRectFn.set(null);
            this.commandFn = null;
            this.isLoading.set(false);
        });
    }

    /**
     * Switches the visible menu into a loading/sub-menu state in-place.
     * Because keepRange items don't call deleteRange, the Tiptap suggestion session
     * stays alive and keyboard routing continues to work without any extra plumbing.
     * subMenuAllItems is cleared so filterItems() returns [] during loading,
     * preventing stale items from leaking through if onUpdate fires before setItems.
     */
    openSubmenu(): void {
        this.isInSubmenu = true;
        this.subMenuAllItems = [];
        this.zone.run(() => {
            // this.items.set([]);
            // this.activeIndex.set(0);
            // this.isLoading.set(true);
            this.commandFn = null;
            // isOpen and clientRectFn unchanged — menu is already visible and positioned
        });
    }

    /** Populates the sub-menu with resolved items and clears the loading state. */
    setItems(items: BlockItem[], commandFn: (item: BlockItem) => void): void {
        this.subMenuAllItems = items; // keep master list for re-filtering as user types
        this.zone.run(() => {
            this.items.set(items);
            this.commandFn = commandFn;
            // this.activeIndex.set(0);
            // this.isLoading.set(false);
        });
    }

    select(item: BlockItem): void {
        // Clicking the floating menu can blur the editor; ProseMirror may then treat the
        // caret as outside the `/…` suggestion range, which deactivates @tiptap/suggestion
        // and fires onExit → close() before this handler runs. Keep the editor focused
        // so the suggestion session (and our commandFn) stay valid for sub-menu picks.
        this.editor?.view.focus();
        this.commandFn?.(item);
    }

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
