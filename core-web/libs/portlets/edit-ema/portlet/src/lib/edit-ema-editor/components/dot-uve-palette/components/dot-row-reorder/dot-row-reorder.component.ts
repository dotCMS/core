import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    Output,
    signal
} from '@angular/core';

import { DotPageAssetLayoutRow } from '@dotcms/types';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-row-reorder',
    standalone: true,
    imports: [CdkDrag, CdkDragHandle, CdkDropList],
    template: `
        @if (rows().length > 0) {
            <div
                cdkDropList
                cdkDropListLockAxis="y"
                (cdkDropListDropped)="drop($event)"
                class="row-reorder-list">
                @for (row of rows(); track $index; let i = $index) {
                    <div cdkDrag class="row-item">
                        <div
                            class="row-header">
                            <div
                                class="row-handle"
                                cdkDragHandle
                                (click)="$event.stopPropagation()">
                                <i class="pi pi-bars"></i>
                            </div>
                            <div
                                class="row-label"
                                (click)="selectRow(i + 1)"
                                (mousedown)="$event.stopPropagation()"
                                (touchstart)="$event.stopPropagation()">
                                {{ getRowLabel(row, i) }}
                            </div>
                            <button
                                type="button"
                                class="row-toggle"
                                [attr.aria-expanded]="isRowExpanded(i)"
                                [attr.aria-label]="
                                    isRowExpanded(i) ? 'Collapse row' : 'Expand row'
                                "
                                (click)="toggleRow(i); $event.stopPropagation()"
                                (mousedown)="$event.stopPropagation()"
                                (touchstart)="$event.stopPropagation()">
                                <i
                                    class="pi"
                                    [class.pi-chevron-down]="isRowExpanded(i)"
                                    [class.pi-chevron-right]="!isRowExpanded(i)"></i>
                            </button>
                        </div>

                        @if (isRowExpanded(i) && row.columns?.length) {
                            <div
                                class="row-body"
                                (mousedown)="$event.stopPropagation()"
                                (touchstart)="$event.stopPropagation()">
                                <div class="row-columns">
                                    @for (column of row.columns; track $index; let j = $index) {
                                        <div class="row-column">
                                            Column {{ j + 1 }}
                                        </div>
                                    }
                                </div>
                            </div>
                        }
                    </div>
                }
            </div>
        } @else {
            <div class="empty-state">
                <span>No rows available</span>
            </div>
        }
    `,
    styles: [`
        .row-reorder-list {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .row-item {
            display: flex;
            flex-direction: column;
            align-items: stretch;
            gap: 0.5rem;
            padding: 0.75rem;
            background: var(--surface-ground);
            border: 1px solid var(--surface-border);
            border-radius: 4px;
            cursor: default;
            transition: box-shadow 0.2s;
        }

        .row-item:hover {
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .row-item.cdk-drag-animating {
            transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
        }

        .row-reorder-list.cdk-drop-list-dragging .row-item:not(.cdk-drag-placeholder) {
            transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
        }

        .row-item.cdk-drag-placeholder {
            opacity: 0.4;
        }

        .row-header {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            min-width: 0;
            cursor: pointer;
        }

        .row-handle {
            display: flex;
            align-items: center;
            color: var(--text-color-secondary);
            cursor: grab;
            user-select: none;
            -webkit-user-select: none;
        }

        .row-handle:active {
            cursor: grabbing;
        }

        .row-label {
            flex: 1;
            font-size: 0.875rem;
            color: var(--text-color);
            min-width: 0;
        }

        .row-body {
            padding-left: 1.75rem;
        }

        .row-columns {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .row-column {
            font-size: 0.8125rem;
            color: var(--text-color);
            background: var(--surface-card);
            border: 1px solid var(--surface-border);
            border-radius: 4px;
            padding: 0.5rem 0.75rem;
        }

        .row-toggle {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            align-self: flex-start;
            width: 2rem;
            height: 2rem;
            margin-left: auto;
            margin-top: 0.125rem;
            border: 1px solid var(--surface-border);
            background: var(--surface-card);
            border-radius: 4px;
            cursor: pointer;
            color: var(--text-color-secondary);
        }

        .row-toggle:hover {
            color: var(--text-color);
        }

        .empty-state {
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 2rem;
            color: var(--text-color-secondary);
        }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRowReorderComponent {
    protected readonly uveStore = inject(UVEStore);

    @Output() onRowSelect = new EventEmitter<{ selector: string; type: string }>();

    private readonly expandedRowIndexes = signal<Set<number>>(new Set());

    protected rows = computed(() => {
        const response = this.uveStore.pageAPIResponse();
        return response?.layout?.body?.rows ?? [];
    });

    protected getRowLabel(row: DotPageAssetLayoutRow, index: number): string {
        return row.styleClass || `Row ${index + 1}`;
    }

    protected selectRow(index: number): void {
        this.onRowSelect.emit({
            selector: `#section-${index}`,
            type: 'row'
        });
    }

    protected isRowExpanded(rowIndex: number): boolean {
        return this.expandedRowIndexes().has(rowIndex);
    }

    protected toggleRow(rowIndex: number): void {
        const next = new Set(this.expandedRowIndexes());
        if (next.has(rowIndex)) {
            next.delete(rowIndex);
        } else {
            next.add(rowIndex);
        }
        this.expandedRowIndexes.set(next);
    }

    protected drop(event: CdkDragDrop<DotPageAssetLayoutRow[]>) {
        const currentRows = this.rows();
        const newRows = [...currentRows];
        moveItemInArray(newRows, event.previousIndex, event.currentIndex);

        this.uveStore.updateRows(newRows);
    }
}
