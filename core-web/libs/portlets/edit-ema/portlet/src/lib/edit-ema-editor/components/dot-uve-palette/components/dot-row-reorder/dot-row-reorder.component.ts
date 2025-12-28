import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, EventEmitter, inject, Output } from '@angular/core';

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
                @for (row of rows(); track row.identifier; let i = $index) {
                    <div cdkDrag class="row-item">
                        <div
                            class="row-handle"
                            cdkDragHandle
                            (click)="$event.stopPropagation()">
                            <i class="pi pi-bars"></i>
                        </div>
                        <!--
                          IMPORTANT:
                          cdkDrag listens on the host for pointer/mouse events.
                          To ensure ONLY the handle can start a drag, we stop propagation here.
                        -->
                        <div
                            class="row-content"
                            (click)="selectRow(i + 1)"
                            (mousedown)="$event.stopPropagation()"
                            (touchstart)="$event.stopPropagation()">
                            <div class="row-label">
                                {{ getRowLabel(row, i) }}
                            </div>
                        </div>
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
            align-items: center;
            gap: 0.75rem;
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

        .row-content {
            flex: 1;
            display: flex;
            min-width: 0;
            cursor: pointer;
        }

        .row-label {
            flex: 1;
            font-size: 0.875rem;
            color: var(--text-color);
            min-width: 0;
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

    protected drop(event: CdkDragDrop<DotPageAssetLayoutRow[]>) {
        const currentRows = this.rows();
        const newRows = [...currentRows];
        moveItemInArray(newRows, event.previousIndex, event.currentIndex);

        this.uveStore.updateRows(newRows);
    }
}
