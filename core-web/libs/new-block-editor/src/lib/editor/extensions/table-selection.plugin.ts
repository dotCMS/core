import { Extension } from '@tiptap/core';
import { Node as PMNode, ResolvedPos } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { TableMap } from '@tiptap/pm/tables';
import { EditorView } from '@tiptap/pm/view';

import type { ActiveCell, TableHandlesStore } from '../services/table-handles.store';

const PLUGIN_KEY = new PluginKey('tableSelectionHandles');

interface TableSelectionOptions {
    store: TableHandlesStore;
}

/**
 * Selection-driven tracker for the floating column / row handles.
 *
 * Phase 3 replacement for the noisy hover-based tracker. Hooks `Plugin.view(view).update`
 * — ProseMirror calls this after every state change (selection move or doc edit) — and
 * pushes the cell containing the cursor to {@link TableHandlesStore}. When the cursor
 * leaves any table, the store is cleared to `null`.
 *
 * Short-circuits when the active cell hasn't changed (same `cellPos`) so signal subscribers
 * don't churn on every keystroke inside a cell.
 */
export const TableSelectionPlugin = Extension.create<TableSelectionOptions>({
    name: 'tableSelectionHandles',

    addOptions() {
        return {
            store: null as unknown as TableHandlesStore
        };
    },

    addProseMirrorPlugins() {
        const store = this.options.store;
        if (!store) return [];

        return [
            new Plugin({
                key: PLUGIN_KEY,
                view: (view) => {
                    // Initial resolve — the editor may already be inside a table on mount.
                    const initial = resolveActiveCell(view);
                    if (initial) store.setActiveCell(initial);

                    return {
                        update: (updatedView) => {
                            const next = resolveActiveCell(updatedView);
                            const current = store.activeCell();
                            if (next?.cellPos === current?.cellPos) return;
                            store.setActiveCell(next);
                        },
                        destroy: () => {
                            store.reset();
                        }
                    };
                }
            })
        ];
    }
});

function resolveActiveCell(view: EditorView): ActiveCell | null {
    const $pos = view.state.selection.$from;
    const tableInfo = findTableAt($pos);
    if (!tableInfo) return null;

    const { tableNode, tablePos, cellPos, rowPos } = tableInfo;
    const map = TableMap.get(tableNode);
    const cellOffset = cellPos - tablePos - 1;
    const cellIndex = map.map.indexOf(cellOffset);
    if (cellIndex < 0) return null;

    const colIndex = cellIndex % map.width;
    const rowIndex = Math.floor(cellIndex / map.width);

    const cellDom = view.nodeDOM(cellPos);
    if (!(cellDom instanceof HTMLElement)) return null;
    const tableEl = cellDom.closest('table');
    if (!(tableEl instanceof HTMLElement)) return null;

    return {
        cellPos,
        rowPos,
        tablePos,
        colIndex,
        rowIndex,
        isHeader: tableNode.nodeAt(cellOffset)?.type.name === 'tableHeader',
        cellEl: cellDom,
        columnHeadEl: findColumnHeadEl(tableEl, colIndex) ?? cellDom,
        rowHeadEl: findRowHeadEl(tableEl, rowIndex) ?? cellDom,
        tableEl
    };
}

interface TableInfo {
    tableNode: PMNode;
    tablePos: number;
    rowPos: number;
    cellPos: number;
}

/**
 * Walks up from a resolved position to find the surrounding table + row + cell. Returns
 * `null` when the position is not inside a table cell. Used instead of `cellAround` so we
 * handle every selection type uniformly (TextSelection, NodeSelection, CellSelection — the
 * resolved `$from` always has a depth path we can traverse).
 */
function findTableAt($pos: ResolvedPos): TableInfo | null {
    for (let depth = $pos.depth; depth > 0; depth--) {
        const node = $pos.node(depth);
        if (node.type.name === 'table') {
            const tablePos = $pos.before(depth);
            const rowDepth = depth + 1;
            const cellDepth = depth + 2;
            if ($pos.depth < cellDepth) return null;
            const rowPos = $pos.before(rowDepth);
            const cellPos = $pos.before(cellDepth);
            return { tableNode: node, tablePos, rowPos, cellPos };
        }
    }
    return null;
}

function findColumnHeadEl(tableEl: HTMLElement, colIndex: number): HTMLElement | null {
    const firstRow = tableEl.querySelector(':scope > tbody > tr') as HTMLElement | null;
    if (!firstRow) return null;
    return (firstRow.children.item(colIndex) as HTMLElement | null) ?? null;
}

function findRowHeadEl(tableEl: HTMLElement, rowIndex: number): HTMLElement | null {
    const rows = tableEl.querySelectorAll(':scope > tbody > tr');
    const row = rows.item(rowIndex) as HTMLElement | null;
    if (!row) return null;
    return (row.children.item(0) as HTMLElement | null) ?? null;
}
