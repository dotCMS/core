import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/**
 * Multi-cell selection popover, opened from the **selection handle** that appears at the
 * right edge of an active `CellSelection`. Two actions:
 *
 *   - **Merge cells** — disabled when the current selection can't be merged.
 *   - **Split cell** — disabled when the active cell isn't a merged cell.
 *
 * Capability state is read reactively from `editor.can()` on every transaction so the
 * disabled-state stays in sync as the user changes the selection.
 */
@Component({
    selector: 'dot-table-selection-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover popoverId="table-selection">
            <div
                role="menu"
                [attr.aria-label]="'dot.block.editor.table.handle.selection.aria-label' | dm"
                class="w-56 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg py-1">
                <button
                    type="button"
                    class="popover-item"
                    [disabled]="!canMerge()"
                    [attr.aria-disabled]="!canMerge()"
                    (mousedown)="action($event, mergeCells)">
                    <span aria-hidden="true" class="material-symbols-outlined">cell_merge</span>
                    <span>{{ 'dot.block.editor.table.selection.merge-cells' | dm }}</span>
                </button>
                <button
                    type="button"
                    class="popover-item"
                    [disabled]="!canSplit()"
                    [attr.aria-disabled]="!canSplit()"
                    (mousedown)="action($event, splitCell)">
                    <span aria-hidden="true" class="material-symbols-outlined">call_split</span>
                    <span>{{ 'dot.block.editor.table.selection.split-cell' | dm }}</span>
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
            .popover-item:hover:not(:disabled) {
                background: rgb(238 242 255);
            }
            .popover-item:disabled {
                color: rgb(156 163 175);
                cursor: not-allowed;
            }
        `
    ]
})
export class TableSelectionPopoverComponent implements OnDestroy {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

    protected readonly canMerge = signal(false);
    protected readonly canSplit = signal(false);

    private cleanup: (() => void) | null = null;

    constructor() {
        // Bind a transaction listener once we have the editor input; keep the capability
        // signals in sync with the current selection.
        effect(() => {
            const ed = this.editor();
            this.cleanup?.();

            const update = () => {
                this.canMerge.set(ed.can().mergeCells());
                this.canSplit.set(ed.can().splitCell());
            };
            update();
            ed.on('transaction', update);
            this.cleanup = () => ed.off('transaction', update);
        });
    }

    ngOnDestroy(): void {
        this.cleanup?.();
    }

    protected action(event: MouseEvent, fn: () => void): void {
        event.preventDefault();
        event.stopPropagation();
        fn();
        this.manager.close();
    }

    protected mergeCells = (): void => {
        this.editor().chain().focus().mergeCells().run();
    };

    protected splitCell = (): void => {
        this.editor().chain().focus().splitCell().run();
    };
}
