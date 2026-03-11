import {
    CdkDrag,
    CdkDragDrop,
    CdkDragHandle,
    CdkDropList,
    moveItemInArray
} from '@angular/cdk/drag-drop';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    output,
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
    templateUrl: './dot-row-reorder.component.html',
    styleUrl: './dot-row-reorder.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRowReorderComponent {
    protected readonly uveStore = inject(UVEStore);

    onRowSelect = output<{ selector: string; type: string }>();

    private readonly expandedRowIndexes = signal<Set<number>>(new Set());
    private readonly columnDragging = signal<boolean>(false);
    protected readonly editRowDialogOpen = signal<boolean>(false);
    private readonly editingRowIndex = signal<number | null>(null);
    protected readonly editingColumn = signal<{ rowIndex: number; columnIndex: number } | null>(
        null
    );

    protected readonly rowStyleClassControl = new FormControl<string>('', { nonNullable: true });

    protected rows = computed(() => {
        const pageLayout = this.uveStore.pageAsset()?.layout;
        return pageLayout?.body?.rows ?? [];
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
            // Removed pageAPIResponse - use normalized accessors
            if (this.uveStore.pageAsset()?.layout) {
                this.uveStore.updateLayout({
                    ...this.uveStore.pageAsset()?.layout,
                    body: {
                        ...this.uveStore.pageAsset()?.layout.body,
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
        // Removed pageAPIResponse - use normalized accessors
        if (this.uveStore.pageAsset()?.layout) {
            this.uveStore.updateLayout({
                ...this.uveStore.pageAsset()?.layout,
                body: {
                    ...this.uveStore.pageAsset()?.layout.body,
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
        // Removed pageAPIResponse - use normalized accessors
        if (!this.uveStore.pageAsset()?.layout) {
            return;
        }

        this.uveStore.updateLayout({
            ...this.uveStore.pageAsset()?.layout,
            body: {
                ...this.uveStore.pageAsset()?.layout.body,
                rows
            }
        });
    }
}
