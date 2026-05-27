import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/**
 * Row-scoped popover, opened from the row handle. Insert above / below, toggle the row
 * as a header row, delete the row. Operates on the cell whose `cellPos` is in the popover
 * payload — snapshotted at open time so concurrent editor selection changes don't move the
 * target out from under the user.
 */
@Component({
    selector: 'dot-table-row-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover popoverId="table-row">
            <div
                role="menu"
                [attr.aria-label]="'dot.block.editor.table.handle.row.aria-label' | dm"
                class="w-56 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg py-1">
                <button
                    type="button"
                    class="popover-item"
                    (mousedown)="action($event, insertAbove)">
                    <span aria-hidden="true" class="material-symbols-outlined">add_row_above</span>
                    <span>{{ 'dot.block.editor.table.row.insert-above' | dm }}</span>
                </button>
                <button
                    type="button"
                    class="popover-item"
                    (mousedown)="action($event, insertBelow)">
                    <span aria-hidden="true" class="material-symbols-outlined">add_row_below</span>
                    <span>{{ 'dot.block.editor.table.row.insert-below' | dm }}</span>
                </button>
                <button
                    type="button"
                    class="popover-item"
                    (mousedown)="action($event, toggleHeader)">
                    <span aria-hidden="true" class="material-symbols-outlined">table_rows</span>
                    <span>{{ 'dot.block.editor.table.row.toggle-header' | dm }}</span>
                </button>
                <button
                    type="button"
                    class="popover-item popover-item--danger"
                    (mousedown)="action($event, deleteRow)">
                    <span aria-hidden="true" class="material-symbols-outlined">delete</span>
                    <span>{{ 'dot.block.editor.table.row.delete' | dm }}</span>
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
        `
    ]
})
export class TableRowPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

    protected action(event: MouseEvent, fn: () => void): void {
        event.preventDefault();
        event.stopPropagation();
        fn();
        this.manager.close();
    }

    private withCell(chain: (editor: Editor) => void): void {
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

    protected insertAbove = (): void => {
        this.withCell((editor) => editor.chain().focus().addRowBefore().run());
    };

    protected insertBelow = (): void => {
        this.withCell((editor) => editor.chain().focus().addRowAfter().run());
    };

    protected toggleHeader = (): void => {
        this.withCell((editor) => editor.chain().focus().toggleHeaderRow().run());
    };

    protected deleteRow = (): void => {
        this.withCell((editor) => editor.chain().focus().deleteRow().run());
    };
}
