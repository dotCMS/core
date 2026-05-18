import {
    ChangeDetectionStrategy,
    Component,
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

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/**
 * Column-scoped popover, opened from the column handle. Provides the actions that apply
 * to the entire column the user is hovering: insert left, insert right, toggle header,
 * delete, plus a header-scope select that only appears when the anchor cell is a `<th>`.
 *
 * The anchor cell position is snapshotted in the popover payload so the actions still
 * target the right column even if the cursor wanders while the popover is open.
 */
@Component({
    selector: 'dot-table-column-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, Select, EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover popoverId="table-column">
            <div
                role="menu"
                [attr.aria-label]="'dot.block.editor.table.handle.column.aria-label' | dm"
                class="w-56 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg py-1">
                <button type="button" class="popover-item" (mousedown)="action($event, insertLeft)">
                    <span aria-hidden="true" class="material-symbols-outlined">add_column_left</span>
                    <span>{{ 'dot.block.editor.table.column.insert-left' | dm }}</span>
                </button>
                <button type="button" class="popover-item" (mousedown)="action($event, insertRight)">
                    <span aria-hidden="true" class="material-symbols-outlined">add_column_right</span>
                    <span>{{ 'dot.block.editor.table.column.insert-right' | dm }}</span>
                </button>
                <button type="button" class="popover-item" (mousedown)="action($event, toggleHeader)">
                    <span aria-hidden="true" class="material-symbols-outlined">view_column</span>
                    <span>{{ 'dot.block.editor.table.column.toggle-header' | dm }}</span>
                </button>

                @if (showScope()) {
                    <div class="popover-row">
                        <label class="popover-row__label" for="tbl-col-scope">
                            {{ 'dot.block.editor.toolbar.table.header-scope' | dm }}
                        </label>
                        <p-select
                            inputId="tbl-col-scope"
                            appendTo="body"
                            [size]="'small'"
                            [options]="scopeOptions"
                            [(ngModel)]="scope"
                            (onChange)="onScopeChange($event.value)" />
                    </div>
                }

                <button
                    type="button"
                    class="popover-item popover-item--danger"
                    (mousedown)="action($event, deleteColumn)">
                    <span aria-hidden="true" class="material-symbols-outlined">delete</span>
                    <span>{{ 'dot.block.editor.table.column.delete' | dm }}</span>
                </button>
            </div>
        </dot-editor-popover>
    `,
    styles: [
        `
            .popover-item {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                width: 100%;
                padding: 0.5rem 0.75rem;
                font-size: 0.875rem;
                color: rgb(55 65 81);
                cursor: pointer;
                background: transparent;
                border: none;
                text-align: left;
            }
            .popover-item:hover {
                background: rgb(238 242 255);
            }
            .popover-item--danger {
                color: rgb(185 28 28);
            }
            .popover-item--danger:hover {
                background: rgb(254 226 226);
            }
            .popover-row {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                padding: 0.5rem 0.75rem;
            }
            .popover-row__label {
                font-size: 0.75rem;
                color: rgb(75 85 99);
                white-space: nowrap;
            }
        `
    ]
})
export class TableColumnPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly scope = signal<string>('');
    protected readonly showScope = computed(
        () => this.manager.tableColumnPayload()?.isHeader ?? false
    );

    protected readonly scopeOptions: ReadonlyArray<{ label: string; value: string }>;

    constructor() {
        const msg = (key: string) => this.dotMessageService.get(key);
        this.scopeOptions = [
            { label: msg('dot.block.editor.toolbar.table.scope.auto'), value: '' },
            { label: msg('dot.block.editor.toolbar.table.scope.col'), value: 'col' },
            { label: msg('dot.block.editor.toolbar.table.scope.row'), value: 'row' },
            { label: msg('dot.block.editor.toolbar.table.scope.colgroup'), value: 'colgroup' },
            { label: msg('dot.block.editor.toolbar.table.scope.rowgroup'), value: 'rowgroup' }
        ];

        // Seed the scope value from the payload whenever the popover opens.
        effect(() => {
            const payload = this.manager.tableColumnPayload();
            const open = this.manager.isOpen('table-column');
            untracked(() => {
                if (open && payload) {
                    this.scope.set(payload.headerScope);
                }
            });
        });
    }

    protected action(event: MouseEvent, fn: () => void): void {
        event.preventDefault();
        event.stopPropagation();
        fn();
        this.manager.close();
    }

    /** Place selection inside the anchor cell, then run a TipTap chain. */
    private withCell(chain: (editor: Editor) => void): void {
        const payload = this.manager.tableColumnPayload();
        if (!payload) return;
        const editor = this.editor();
        editor.chain().focus().setTextSelection(payload.cellPos + 1).run();
        chain(editor);
    }

    protected insertLeft = (): void => {
        this.withCell((editor) => editor.chain().focus().addColumnBefore().run());
    };

    protected insertRight = (): void => {
        this.withCell((editor) => editor.chain().focus().addColumnAfter().run());
    };

    protected toggleHeader = (): void => {
        this.withCell((editor) => editor.chain().focus().toggleHeaderColumn().run());
    };

    protected deleteColumn = (): void => {
        this.withCell((editor) => editor.chain().focus().deleteColumn().run());
    };

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
