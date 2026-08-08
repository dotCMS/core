import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    Signal,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Select } from 'primeng/select';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { FULLSCREEN_AWARE_OVERLAY_OPTIONS } from '../../config.utils';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/**
 * Unified popover for the three table handles (column / row / multi-cell selection). One
 * `<dot-editor-popover>` shell is bound to all three popover ids; the rendered actions are
 * picked from `COLUMN_ACTIONS`, `ROW_ACTIONS` or `SELECTION_ACTIONS` based on which id is
 * currently active. Only one is open at a time — the `EditorPopoverService.activePopover`
 * signal already enforces that — so a single component naturally owns all three variants
 * with no behavior change versus the previous three-component setup.
 *
 * Adding a new action: drop an entry into {@link ACTIONS} and append the key to the right
 * action list. The template's `@for` picks it up automatically.
 *
 * Two pieces of UI live outside the action loop:
 *   - The column variant's scope `<p-select>` (only shown when the active cell is `<th>`).
 *   - The selection variant's reactive `disabled` state, fed by `editor.can()`.
 */

type ActionKey =
    | 'columnInsertLeft'
    | 'columnInsertRight'
    | 'columnToggleHeader'
    | 'columnDelete'
    | 'rowInsertAbove'
    | 'rowInsertBelow'
    | 'rowToggleHeader'
    | 'rowDelete'
    | 'selectionMerge'
    | 'selectionSplit';

interface ActionEntry {
    testid: string;
    icon: string;
    labelKey: string;
    handler: () => void;
    variant?: 'danger';
    disabled?: Signal<boolean>;
}

@Component({
    selector: 'dot-table-handle-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, Select, EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover [popoverId]="POPOVER_IDS">
            <div
                role="menu"
                [attr.aria-label]="activeAriaLabel() | dm"
                class="w-56 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
                @for (key of currentActions(); track key) {
                    @let action = ACTIONS[key];
                    <button
                        type="button"
                        [class]="actionClass(action)"
                        [disabled]="action.disabled?.() ?? false"
                        [attr.aria-disabled]="action.disabled?.() ?? false"
                        [attr.data-testid]="action.testid"
                        (mousedown)="run($event, action.handler)">
                        <span aria-hidden="true" class="material-symbols-outlined">
                            {{ action.icon }}
                        </span>
                        <span>{{ action.labelKey | dm }}</span>
                    </button>
                }

                @if (showScopeSelect()) {
                    <div class="flex flex-col gap-1 px-3 py-2">
                        <label class="text-xs font-medium text-gray-700" for="tbl-col-scope">
                            {{ 'dot.block.editor.toolbar.table.header-scope' | dm }}
                        </label>
                        <p-select
                            inputId="tbl-col-scope"
                            appendTo="body"
                            [overlayOptions]="overlayOptions"
                            styleClass="popover-field__select"
                            [size]="'small'"
                            [options]="scopeOptions"
                            [(ngModel)]="scope"
                            (onChange)="onScopeChange($event.value)" />
                    </div>
                }
            </div>
        </dot-editor-popover>
    `,
    styles: [
        `
            /*
             * PrimeNG escape hatch — can't be Tailwind. The component renders its own root
             * with class .p-select; we need to stretch that element to fill the popover.
             * styleClass="popover-field__select" lands on the .p-select root, then this
             * rule pierces Angular view encapsulation to override PrimeNG's default width.
             */
            :host ::ng-deep .popover-field__select.p-select,
            :host ::ng-deep .popover-field__select .p-select {
                width: 100%;
            }
        `
    ]
})
export class TableHandlePopoverComponent implements OnDestroy {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly POPOVER_IDS = ['table-column', 'table-row', 'table-selection'] as const;

    /**
     * Overlay options for the header-scope `<p-select>` panel, which appends to `document.body`.
     * Lifts it above the fullscreen editor shell's `z-[9998]` backdrop so the dropdown stays
     * clickable in fullscreen. See {@link FULLSCREEN_AWARE_OVERLAY_OPTIONS}.
     */
    protected readonly overlayOptions = FULLSCREEN_AWARE_OVERLAY_OPTIONS;

    // ── Action lists ─────────────────────────────────────────────────────────
    // Plain lists of action keys per variant. The actual config lives in `ACTIONS` below.

    protected readonly COLUMN_ACTIONS: readonly ActionKey[] = [
        'columnInsertLeft',
        'columnInsertRight',
        'columnToggleHeader',
        'columnDelete'
    ];

    protected readonly ROW_ACTIONS: readonly ActionKey[] = [
        'rowInsertAbove',
        'rowInsertBelow',
        'rowToggleHeader',
        'rowDelete'
    ];

    protected readonly SELECTION_ACTIONS: readonly ActionKey[] = [
        'selectionMerge',
        'selectionSplit'
    ];

    protected readonly currentActions = computed<readonly ActionKey[]>(() => {
        const id = this.manager.activePopover()?.id;
        if (id === 'table-column') return this.COLUMN_ACTIONS;
        if (id === 'table-row') return this.ROW_ACTIONS;
        if (id === 'table-selection') return this.SELECTION_ACTIONS;
        return [];
    });

    protected readonly activeAriaLabel = computed(() => {
        const id = this.manager.activePopover()?.id;
        if (id === 'table-column') return 'dot.block.editor.table.handle.column.aria-label';
        if (id === 'table-row') return 'dot.block.editor.table.handle.row.aria-label';
        if (id === 'table-selection') return 'dot.block.editor.table.handle.selection.aria-label';
        return '';
    });

    // ── Selection variant: reactive can-merge / can-split ────────────────────

    protected readonly canMerge = signal(false);
    protected readonly canSplit = signal(false);
    private readonly canNotMerge = computed(() => !this.canMerge());
    private readonly canNotSplit = computed(() => !this.canSplit());

    // ── Column variant: scope select state ───────────────────────────────────

    protected readonly scope = signal<string>('');
    protected readonly showScopeSelect = computed(
        () =>
            this.manager.activePopover()?.id === 'table-column' &&
            (this.manager.tableColumnPayload()?.isHeader ?? false)
    );
    protected readonly scopeOptions: ReadonlyArray<{ label: string; value: string }>;

    // ── Action registry ──────────────────────────────────────────────────────

    protected readonly ACTIONS: Record<ActionKey, ActionEntry> = {
        columnInsertLeft: {
            testid: 'col-insert-left',
            icon: 'add_column_left',
            labelKey: 'dot.block.editor.table.column.insert-left',
            handler: () => this.runColumnCommand((ed) => ed.chain().focus().addColumnBefore().run())
        },
        columnInsertRight: {
            testid: 'col-insert-right',
            icon: 'add_column_right',
            labelKey: 'dot.block.editor.table.column.insert-right',
            handler: () => this.runColumnCommand((ed) => ed.chain().focus().addColumnAfter().run())
        },
        columnToggleHeader: {
            testid: 'col-toggle-header',
            icon: 'view_column',
            labelKey: 'dot.block.editor.table.column.toggle-header',
            handler: () =>
                this.runColumnCommand((ed) => ed.chain().focus().toggleHeaderColumn().run())
        },
        columnDelete: {
            testid: 'col-delete',
            icon: 'delete',
            labelKey: 'dot.block.editor.table.column.delete',
            variant: 'danger',
            handler: () => this.runColumnCommand((ed) => ed.chain().focus().deleteColumn().run())
        },
        rowInsertAbove: {
            testid: 'row-insert-above',
            icon: 'add_row_above',
            labelKey: 'dot.block.editor.table.row.insert-above',
            handler: () => this.runRowCommand((ed) => ed.chain().focus().addRowBefore().run())
        },
        rowInsertBelow: {
            testid: 'row-insert-below',
            icon: 'add_row_below',
            labelKey: 'dot.block.editor.table.row.insert-below',
            handler: () => this.runRowCommand((ed) => ed.chain().focus().addRowAfter().run())
        },
        rowToggleHeader: {
            testid: 'row-toggle-header',
            icon: 'table_rows',
            labelKey: 'dot.block.editor.table.row.toggle-header',
            handler: () => this.runRowCommand((ed) => ed.chain().focus().toggleHeaderRow().run())
        },
        rowDelete: {
            testid: 'row-delete',
            icon: 'delete',
            labelKey: 'dot.block.editor.table.row.delete',
            variant: 'danger',
            handler: () => this.runRowCommand((ed) => ed.chain().focus().deleteRow().run())
        },
        selectionMerge: {
            testid: 'selection-merge',
            icon: 'cell_merge',
            labelKey: 'dot.block.editor.table.selection.merge-cells',
            disabled: this.canNotMerge,
            handler: () => this.editor().chain().focus().mergeCells().run()
        },
        selectionSplit: {
            testid: 'selection-split',
            icon: 'call_split',
            labelKey: 'dot.block.editor.table.selection.split-cell',
            disabled: this.canNotSplit,
            handler: () => this.editor().chain().focus().splitCell().run()
        }
    };

    private cleanupCanListener: (() => void) | null = null;

    constructor() {
        const msg = (key: string) => this.dotMessageService.get(key);
        this.scopeOptions = [
            { label: msg('dot.block.editor.toolbar.table.scope.auto'), value: '' },
            { label: msg('dot.block.editor.toolbar.table.scope.col'), value: 'col' },
            { label: msg('dot.block.editor.toolbar.table.scope.row'), value: 'row' },
            { label: msg('dot.block.editor.toolbar.table.scope.colgroup'), value: 'colgroup' },
            { label: msg('dot.block.editor.toolbar.table.scope.rowgroup'), value: 'rowgroup' }
        ];

        // Seed scope from the column payload when the popover opens for the column variant.
        effect(() => {
            const payload = this.manager.tableColumnPayload();
            const open = this.manager.isOpen('table-column');
            untracked(() => {
                if (open && payload) this.scope.set(payload.headerScope);
            });
        });

        // Mirror editor.can().mergeCells() / splitCell() to signals so the selection-variant
        // action entries can bind a reactive `disabled` state.
        effect(() => {
            const ed = this.editor();
            this.cleanupCanListener?.();

            const update = () => {
                this.canMerge.set(ed.can().mergeCells());
                this.canSplit.set(ed.can().splitCell());
            };
            update();
            ed.on('transaction', update);
            this.cleanupCanListener = () => ed.off('transaction', update);
        });
    }

    ngOnDestroy(): void {
        this.cleanupCanListener?.();
    }

    // ── Click + command helpers ──────────────────────────────────────────────

    /**
     * Tailwind utility classes for a menu-item button. Splits the static base classes from
     * the per-variant color set so the disabled / hover states cascade cleanly without
     * fighting `:disabled` ordering rules.
     */
    protected actionClass(action: ActionEntry): string {
        const base =
            'flex w-full cursor-pointer items-center gap-2 border-0 bg-transparent px-3 py-2 text-left text-sm disabled:cursor-not-allowed disabled:hover:bg-transparent';
        const variant =
            action.variant === 'danger'
                ? 'text-red-700 hover:bg-red-100 disabled:text-red-300'
                : 'text-gray-700 hover:bg-indigo-50 disabled:text-gray-400';
        return `${base} ${variant}`;
    }

    protected run(event: MouseEvent, handler: () => void): void {
        event.preventDefault();
        event.stopPropagation();
        handler();
        this.manager.close();
    }

    /** Place selection inside the column-payload's cell, then run the chain. */
    private runColumnCommand(chain: (editor: Editor) => void): void {
        const payload = this.manager.tableColumnPayload();
        if (!payload) return;
        const editor = this.editor();
        editor
            .chain()
            .focus()
            .setTextSelection(payload.cellPos + 1)
            .run();
        chain(editor);
    }

    /** Place selection inside the row-payload's cell, then run the chain. */
    private runRowCommand(chain: (editor: Editor) => void): void {
        const payload = this.manager.tableRowPayload();
        if (!payload) return;
        const editor = this.editor();
        editor
            .chain()
            .focus()
            .setTextSelection(payload.cellPos + 1)
            .run();
        chain(editor);
    }

    protected onScopeChange(value: string): void {
        const payload = this.manager.tableColumnPayload();
        if (!payload) return;
        this.editor()
            .chain()
            .focus()
            .setTextSelection(payload.cellPos + 1)
            .updateAttributes('tableHeader', { scope: value === '' ? null : value })
            .run();
    }
}
