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
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageAssetLayoutColumn, DotPageAssetLayoutRow } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../../../store/dot-uve.store';

/** Payload emitted when the user selects a layout row in the palette (for editor focus). */
export interface DotRowReorderSelectEvent {
    selector: string;
    type: string;
}

/**
 * Palette tree for the Universal Visual Editor: reorder rows and columns via drag-and-drop,
 * expand/collapse sections, and rename rows or columns through a shared dialog.
 *
 * Mutations call {@link UVEStore.updateLayout} for immediate UI feedback and
 * {@link UVEStore.updateRows} to persist the row model.
 */
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
        DotMessagePipe,
        ButtonModule,
        TooltipModule
    ],
    templateUrl: './dot-row-reorder.component.html',
    styles: [
        `
            .cdk-drag-animating {
                transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
            }
            .cdk-drop-list-dragging .cdk-drag:not(.cdk-drag-placeholder) {
                transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
            }
            .cdk-drag-placeholder {
                opacity: 0.4;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRowReorderComponent {
    protected readonly uveStore = inject(UVEStore);

    /** Fired when the user clicks a row label to focus that section in the editor. */
    readonly onRowSelect = output<DotRowReorderSelectEvent>();

    // --- Edit dialog (row or column name) ---

    readonly editRowDialogOpen = signal<boolean>(false);
    readonly editingColumn = signal<{ rowIndex: number; columnIndex: number } | null>(null);
    readonly rowNameControl = new FormControl<string>('', { nonNullable: true });

    private readonly editingRowIndex = signal<number | null>(null);

    // --- Expand/collapse ---

    private readonly expandedRowIndexes = signal<Set<number>>(new Set());
    private readonly expandedColumnKeys = signal<Set<string>>(new Set());

    // --- Drag state (disables row drag while a column is dragging) ---

    private readonly columnDragging = signal<boolean>(false);

    /** Layout rows from the current page asset (empty array when none). */
    readonly rows = computed(() => {
        const pageLayout = this.uveStore.pageAsset()?.layout;
        return pageLayout?.body?.rows ?? [];
    });

    /**
     * Display label for a row: metadata name or a default "Row N".
     */
    getRowLabel(row: DotPageAssetLayoutRow, index: number): string {
        return this.metadataDisplayName(row.metadata) || `Row ${index + 1}`;
    }

    /**
     * Display label for a column: metadata name or a default "Column N".
     */
    getColumnLabel(column: DotPageAssetLayoutColumn, index: number): string {
        return this.metadataDisplayName(column.metadata) || `Column ${index + 1}`;
    }

    /**
     * Resolves each column container to a display title using page asset container data.
     */
    getColumnContainers(column: DotPageAssetLayoutColumn): { title: string }[] {
        const containersData = this.uveStore.pageAsset()?.containers ?? {};

        return (column.containers ?? []).map(({ identifier }) => {
            const title = containersData[identifier]?.container?.title ?? identifier;
            return { title };
        });
    }

    /**
     * Notifies the parent to select the row in the iframe/editor.
     * @param sectionIndex 1-based index used in the DOM selector (`#section-{n}`).
     */
    protected selectRow(sectionIndex: number): void {
        this.onRowSelect.emit({
            selector: `#section-${sectionIndex}`,
            type: 'row'
        });
    }

    /** Opens the rename dialog for the given row. */
    protected openEditRowDialog(rowIndex: number): void {
        const row = this.rows()[rowIndex];
        this.editingRowIndex.set(rowIndex);
        this.editingColumn.set(null);
        this.rowNameControl.setValue(this.metadataDisplayName(row?.metadata) ?? '');
        this.editRowDialogOpen.set(true);
    }

    /** Opens the rename dialog for a column within a row. */
    protected openEditColumnDialog(rowIndex: number, columnIndex: number): void {
        const row = this.rows()[rowIndex];
        const column = row?.columns?.[columnIndex];

        this.editingRowIndex.set(null);
        this.editingColumn.set({ rowIndex, columnIndex });
        this.rowNameControl.setValue(this.metadataDisplayName(column?.metadata) ?? '');
        this.editRowDialogOpen.set(true);
    }

    /** Resets dialog state when the dialog is closed or after a successful save. */
    protected closeEditRowDialog(): void {
        this.editRowDialogOpen.set(false);
        this.editingRowIndex.set(null);
        this.editingColumn.set(null);
    }

    /**
     * Persists the trimmed name from the dialog to either the active column or row,
     * then syncs layout + rows on the store (optimistic UI).
     */
    protected submitEditRow(): void {
        const nextName = this.rowNameControl.value.trim();
        const currentRows = this.rows();
        const columnEdit = this.editingColumn();
        const rowEditIndex = this.editingRowIndex();

        if (columnEdit) {
            const updatedRows = this.replaceColumnMetadataName(
                currentRows,
                columnEdit.rowIndex,
                columnEdit.columnIndex,
                nextName
            );
            if (!updatedRows) {
                return;
            }
            this.persistRows(updatedRows);
            this.closeEditRowDialog();
            return;
        }

        if (rowEditIndex === null || !currentRows[rowEditIndex]) {
            return;
        }

        const updatedRows = this.replaceRowMetadataName(currentRows, rowEditIndex, nextName);
        this.persistRows(updatedRows);
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

    protected isColumnExpanded(rowIndex: number, colIndex: number): boolean {
        return this.expandedColumnKeys().has(this.columnExpansionKey(rowIndex, colIndex));
    }

    protected toggleColumn(rowIndex: number, colIndex: number): void {
        const key = this.columnExpansionKey(rowIndex, colIndex);
        const next = new Set(this.expandedColumnKeys());
        if (next.has(key)) {
            next.delete(key);
        } else {
            next.add(key);
        }
        this.expandedColumnKeys.set(next);
    }

    protected isColumnDragging(): boolean {
        return this.columnDragging();
    }

    protected setColumnDragging(isDragging: boolean): void {
        this.columnDragging.set(isDragging);
    }

    /** Handles reordering top-level layout rows. */
    protected drop(event: CdkDragDrop<DotPageAssetLayoutRow[]>): void {
        const currentRows = this.rows();
        const newRows = [...currentRows];
        moveItemInArray(newRows, event.previousIndex, event.currentIndex);
        this.persistRows(newRows);
    }

    /**
     * Reorders columns within a single row and recomputes `leftOffset` from column widths.
     * Ignores cross-list drops.
     */
    protected dropColumn(event: CdkDragDrop<DotPageAssetLayoutColumn[]>, rowIndex: number): void {
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

        const newRows = currentRows.map((row, idx) =>
            idx === rowIndex ? { ...row, columns: updatedColumns } : row
        );

        this.persistRows(newRows);
    }

    /**
     * After reordering columns, assigns sequential `leftOffset` values from each column's `width`.
     */
    private recomputeLeftOffsets(columns: DotPageAssetLayoutColumn[]): DotPageAssetLayoutColumn[] {
        let offset = 1;

        return columns.map((column) => {
            const width = Math.max(0, column.width ?? 0);
            const next = { ...column, leftOffset: offset };
            offset += width;
            return next;
        });
    }

    /**
     * Writes rows into `pageAsset.layout` (when present) and delegates to the store row updater.
     */
    private optimisticUpdateRows(rows: DotPageAssetLayoutRow[]): void {
        const layout = this.uveStore.pageAsset()?.layout;
        if (!layout) {
            return;
        }

        this.uveStore.updateLayout({
            ...layout,
            body: {
                ...layout.body,
                rows
            }
        });
    }

    /** Applies optimistic layout patch and persists rows on the store. */
    private persistRows(rows: DotPageAssetLayoutRow[]): void {
        this.optimisticUpdateRows(rows);
        this.uveStore.updateRows(rows);
    }

    private metadataDisplayName(metadata: DotPageAssetLayoutRow['metadata']): string | undefined {
        const name = metadata?.['name'];
        return typeof name === 'string' ? name : undefined;
    }

    private columnExpansionKey(rowIndex: number, colIndex: number): string {
        return `${rowIndex}-${colIndex}`;
    }

    /**
     * Returns a new rows array with the column's `metadata.name` updated, or `undefined` if indices are invalid.
     */
    private replaceColumnMetadataName(
        currentRows: DotPageAssetLayoutRow[],
        rowIndex: number,
        columnIndex: number,
        nextName: string
    ): DotPageAssetLayoutRow[] | undefined {
        const row = currentRows[rowIndex];
        const column = row?.columns?.[columnIndex];

        if (!row || !column) {
            return undefined;
        }

        return currentRows.map((r, rIdx) => {
            if (rIdx !== rowIndex) {
                return r;
            }

            const updatedColumns = (r.columns ?? []).map((c, cIdx) =>
                cIdx === columnIndex
                    ? {
                          ...c,
                          metadata: { ...(c.metadata ?? {}), name: nextName || undefined }
                      }
                    : c
            );

            return { ...r, columns: updatedColumns };
        });
    }

    private replaceRowMetadataName(
        currentRows: DotPageAssetLayoutRow[],
        rowIndex: number,
        nextName: string
    ): DotPageAssetLayoutRow[] {
        return currentRows.map((row, idx) =>
            idx === rowIndex
                ? { ...row, metadata: { ...(row.metadata ?? {}), name: nextName || undefined } }
                : row
        );
    }
}
