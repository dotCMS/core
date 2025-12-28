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
import { ReactiveFormsModule, FormControl } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotPageAssetLayoutColumn, DotPageAssetLayoutRow } from '@dotcms/types';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-row-reorder',
    standalone: true,
    imports: [
        CdkDrag,
        CdkDragHandle,
        CdkDropList,
        DialogModule,
        ReactiveFormsModule,
        InputTextModule,
        ButtonModule
    ],
    template: `
        @if (rows().length > 0) {
            <div
                cdkDropList
                cdkDropListLockAxis="y"
                (cdkDropListDropped)="drop($event)"
                class="row-reorder-list">
                @for (row of rows(); track $index; let i = $index) {
                    <div cdkDrag class="row-item" [cdkDragDisabled]="isColumnDragging()">
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
                            (dblclick)="openEditRowDialog(i); $event.stopPropagation()"
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
                                <div
                                    cdkDropList
                                    cdkDropListLockAxis="y"
                                    [cdkDropListData]="row.columns"
                                    (cdkDropListDropped)="dropColumn($event, i)"
                                    class="row-columns">
                                    @for (column of row.columns; track $index; let j = $index) {
                                        <div
                                            cdkDrag
                                            class="row-column"
                                            (cdkDragStarted)="setColumnDragging(true)"
                                            (cdkDragEnded)="setColumnDragging(false)">
                                            <div class="column-handle" cdkDragHandle>
                                                <i class="pi pi-bars"></i>
                                            </div>
                                            <div
                                                class="column-label"
                                                (dblclick)="openEditColumnDialog(i, j); $event.stopPropagation()"
                                                (mousedown)="$event.stopPropagation()"
                                                (touchstart)="$event.stopPropagation()">
                                                {{ getColumnLabel(column, j) }}
                                            </div>
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

        <p-dialog
            [header]="editingColumn() ? 'Edit Column' : 'Edit Row'"
            [modal]="true"
            [draggable]="false"
            [resizable]="false"
            [(visible)]="editRowDialogOpen"
            (onHide)="closeEditRowDialog()">
            <form class="row-edit-form" (submit)="$event.preventDefault(); submitEditRow()">
                <label class="row-edit-label" for="row-styleClass">name</label>
                <input
                    id="row-styleClass"
                    type="text"
                    pInputText
                    [formControl]="rowStyleClassControl" />

                <div class="row-edit-actions">
                    <button pButton type="submit" label="Submit"></button>
                </div>
            </form>
        </p-dialog>
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
            display: flex;
            align-items: center;
            gap: 0.5rem;
            font-size: 0.8125rem;
            color: var(--text-color);
            background: var(--surface-card);
            border: 1px solid var(--surface-border);
            border-radius: 4px;
            padding: 0.5rem 0.75rem;
        }

        .column-handle {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: var(--text-color-secondary);
            cursor: grab;
            user-select: none;
            -webkit-user-select: none;
        }

        .column-handle:active {
            cursor: grabbing;
        }

        .column-label {
            flex: 1;
            min-width: 0;
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

        .row-edit-form {
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
            min-width: 20rem;
        }

        .row-edit-label {
            font-size: 0.875rem;
            color: var(--text-color);
        }

        .row-edit-actions {
            display: flex;
            justify-content: flex-end;
        }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRowReorderComponent {
    protected readonly uveStore = inject(UVEStore);

    @Output() onRowSelect = new EventEmitter<{ selector: string; type: string }>();

    private readonly expandedRowIndexes = signal<Set<number>>(new Set());
    private readonly columnDragging = signal<boolean>(false);
    protected readonly editRowDialogOpen = signal<boolean>(false);
    private readonly editingRowIndex = signal<number | null>(null);
    protected readonly editingColumn = signal<{ rowIndex: number; columnIndex: number } | null>(null);

    protected readonly rowStyleClassControl = new FormControl<string>('', { nonNullable: true });

    protected rows = computed(() => {
        const response = this.uveStore.pageAPIResponse();
        return response?.layout?.body?.rows ?? [];
    });

    protected getRowLabel(row: DotPageAssetLayoutRow, index: number): string {
        return row.styleClass || `Row ${index + 1}`;
    }

    protected getColumnLabel(column: DotPageAssetLayoutColumn, index: number): string {
        return column.styleClass || `Column ${index + 1}`;
    }

    protected selectRow(index: number): void {
        this.onRowSelect.emit({
            selector: `#section-${index}`,
            type: 'row'
        });
    }

    protected openEditRowDialog(rowIndex: number): void {
        const row = this.rows()[rowIndex];
        this.editingRowIndex.set(rowIndex);
        this.editingColumn.set(null);
        this.rowStyleClassControl.setValue(row?.styleClass ?? '');
        this.editRowDialogOpen.set(true);
    }

    protected openEditColumnDialog(rowIndex: number, columnIndex: number): void {
        const row = this.rows()[rowIndex];
        const column = row?.columns?.[columnIndex];

        this.editingRowIndex.set(null);
        this.editingColumn.set({ rowIndex, columnIndex });
        this.rowStyleClassControl.setValue(column?.styleClass ?? '');
        this.editRowDialogOpen.set(true);
    }

    protected closeEditRowDialog(): void {
        this.editRowDialogOpen.set(false);
        this.editingRowIndex.set(null);
        this.editingColumn.set(null);
    }

    protected submitEditRow(): void {
        const nextStyleClass = this.rowStyleClassControl.value.trim();

        const currentRows = this.rows();
        const columnEdit = this.editingColumn();
        const rowEditIndex = this.editingRowIndex();

        if (columnEdit) {
            const { rowIndex, columnIndex } = columnEdit;
            const row = currentRows[rowIndex];
            const column = row?.columns?.[columnIndex];

            if (!row || !column) {
                return;
            }

            const updatedRows = currentRows.map((r, rIdx) => {
                if (rIdx !== rowIndex) {
                    return r;
                }

                const updatedColumns = (r.columns ?? []).map((c, cIdx) => {
                    return cIdx === columnIndex
                        ? { ...c, styleClass: nextStyleClass || undefined }
                        : c;
                });

                return { ...r, columns: updatedColumns };
            });

            // Optimistic UI update
            const pageResponse = this.uveStore.pageAPIResponse();
            if (pageResponse?.layout) {
                this.uveStore.updateLayout({
                    ...pageResponse.layout,
                    body: {
                        ...pageResponse.layout.body,
                        rows: updatedRows
                    }
                });
            }

            this.uveStore.updateRows(updatedRows);
            this.closeEditRowDialog();
            return;
        }

        if (rowEditIndex === null || !currentRows[rowEditIndex]) {
            return;
        }

        const updatedRows = currentRows.map((row, idx) => {
            return idx === rowEditIndex ? { ...row, styleClass: nextStyleClass || undefined } : row;
        });

        // Optimistic UI update (so the label changes immediately)
        const pageResponse = this.uveStore.pageAPIResponse();
        if (pageResponse?.layout) {
            this.uveStore.updateLayout({
                ...pageResponse.layout,
                body: {
                    ...pageResponse.layout.body,
                    rows: updatedRows
                }
            });
        }

        this.uveStore.updateRows(updatedRows);
        this.closeEditRowDialog();
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

    protected isColumnDragging(): boolean {
        return this.columnDragging();
    }

    protected setColumnDragging(isDragging: boolean): void {
        this.columnDragging.set(isDragging);
    }

    protected drop(event: CdkDragDrop<DotPageAssetLayoutRow[]>) {
        const currentRows = this.rows();
        const newRows = [...currentRows];
        moveItemInArray(newRows, event.previousIndex, event.currentIndex);

        this.optimisticUpdateRows(newRows);
        this.uveStore.updateRows(newRows);
    }

    protected dropColumn(event: CdkDragDrop<DotPageAssetLayoutColumn[]>, rowIndex: number) {
        if (event.previousContainer !== event.container) {
            return;
        }

        const currentRows = this.rows();
        const targetRow = currentRows[rowIndex];

        if (!targetRow?.columns) {
            return;
        }

        const newColumns = [...targetRow.columns];
        moveItemInArray(newColumns, event.previousIndex, event.currentIndex);
        const updatedColumns = this.recomputeLeftOffsets(newColumns);

        const newRows = currentRows.map((row, idx) => {
            return idx === rowIndex ? { ...row, columns: updatedColumns } : row;
        });

        this.optimisticUpdateRows(newRows);
        this.uveStore.updateRows(newRows);
    }

    private recomputeLeftOffsets(columns: DotPageAssetLayoutColumn[]): DotPageAssetLayoutColumn[] {
        let offset = 1;

        return columns.map((column) => {
            const width = Math.max(0, column.width ?? 0);
            const next = { ...column, leftOffset: offset };
            offset += width;

            return next;
        });
    }

    private optimisticUpdateRows(rows: DotPageAssetLayoutRow[]): void {
        const pageResponse = this.uveStore.pageAPIResponse();
        if (!pageResponse?.layout) {
            return;
        }

        this.uveStore.updateLayout({
            ...pageResponse.layout,
            body: {
                ...pageResponse.layout.body,
                rows
            }
        });
    }
}
