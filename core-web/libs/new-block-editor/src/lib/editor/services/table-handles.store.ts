import { Injectable, NgZone, computed, inject, signal } from '@angular/core';

/**
 * Snapshot of the cell currently containing the editor cursor. Pushed by
 * {@link TableSelectionPlugin} on every relevant `view.update`; consumed by the floating
 * column / row handles to compute their anchor positions.
 */
export interface ActiveCell {
    /** Document position of the cell node. */
    readonly cellPos: number;
    /** Document position of the row that owns the cell. */
    readonly rowPos: number;
    /** Document position of the table that owns the row. */
    readonly tablePos: number;
    /** Zero-indexed column position within the table. */
    readonly colIndex: number;
    /** Zero-indexed row position within the table. */
    readonly rowIndex: number;
    /** True when the cell node is `tableHeader` (not `tableCell`). */
    readonly isHeader: boolean;
    /** The cell's own DOM element. */
    readonly cellEl: HTMLElement;
    /** First-row cell of this column (column handle anchors here). */
    readonly columnHeadEl: HTMLElement;
    /** First cell of this row (row handle anchors here). */
    readonly rowHeadEl: HTMLElement;
    /** The `<table>` element this cell belongs to. */
    readonly tableEl: HTMLElement;
}

/**
 * Tracks the cell the editor cursor is currently inside. Updated by the selection plugin
 * whenever a `view.update` resolves a new cell; cleared to `null` when the cursor leaves
 * any table.
 *
 * Selection-driven (Phase 3) — replaces the hover-driven model from Phase 2. There is no
 * grace period or lock state: the handle buttons themselves use `mousedown.preventDefault`
 * to keep the cursor inside the cell, so the store's value naturally stays put while the
 * user interacts with the floating popovers.
 *
 * Provided at the editor component scope so each editor instance has its own state.
 */
@Injectable()
export class TableHandlesStore {
    private readonly zone = inject(NgZone);

    private readonly _activeCell = signal<ActiveCell | null>(null);

    /** Current cell holding the cursor, or `null` when the cursor is outside any table. */
    readonly activeCell = this._activeCell.asReadonly();

    /** True when the handles should be rendered. */
    readonly isVisible = computed(() => this._activeCell() !== null);

    setActiveCell(cell: ActiveCell | null): void {
        this.zone.run(() => this._activeCell.set(cell));
    }

    reset(): void {
        this._activeCell.set(null);
    }
}
