import { Extension } from '@tiptap/core';
import { Node as PMNode, ResolvedPos } from '@tiptap/pm/model';
import { EditorState, Plugin, PluginKey } from '@tiptap/pm/state';
import { CellSelection, TableMap } from '@tiptap/pm/tables';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

const PLUGIN_KEY = new PluginKey<DecorationSet>('tableActiveCells');

/**
 * Marks cells with CSS classes that drive which handles the NodeView reveals:
 *
 *   - **Single-cell selection** (caret in one cell): every cell in the cursor's column gets
 *     `is-active-column`, every cell in the cursor's row gets `is-active-row`. CSS shows the
 *     column handle on the first-row cell of the active column, and the row handle on the
 *     first-column cell of the active row.
 *   - **Multi-cell `CellSelection`** (drag-select across 2+ cells): same column / row
 *     decorations anchored to the selection's **anchor cell**, PLUS an `is-selection-anchor`
 *     decoration on a chosen middle-row cell of the selection's right column. CSS surfaces
 *     all three handles (column / row / selection-merge) simultaneously.
 *
 * `prosemirror-tables` already composes multiple `Decoration.node` classes on the same cell
 * (e.g. `selectedCell`), so adding our own classes never clobbers theirs.
 */
export const TableActiveCellsPlugin = Extension.create({
    name: 'tableActiveCells',

    addProseMirrorPlugins() {
        return [
            new Plugin<DecorationSet>({
                key: PLUGIN_KEY,
                state: {
                    init: (_, state) => buildDecorations(state),
                    apply: (tr, oldSet, _oldState, newState) => {
                        // Only recompute when the selection moved or the doc changed —
                        // skipping pure metadata transactions keeps this cheap.
                        if (!tr.docChanged && !tr.selectionSet) return oldSet;
                        return buildDecorations(newState);
                    }
                },
                props: {
                    decorations(state) {
                        return PLUGIN_KEY.getState(state) ?? null;
                    }
                }
            })
        ];
    }
});

function buildDecorations(state: EditorState): DecorationSet {
    // For a CellSelection, `state.selection.$from` resolves at the row depth (not inside
    // a cell), so `findTableAt` would return null. Use `$anchorCell` instead — it's a
    // resolved position whose depth IS at the row, and whose `pos` points right before
    // the anchor cell.
    const isCellSelection = state.selection instanceof CellSelection;
    const tableInfo = isCellSelection
        ? findTableFromCellAnchor((state.selection as CellSelection).$anchorCell)
        : findTableAt(state.selection.$from);
    if (!tableInfo) return DecorationSet.empty;

    const { tableNode, tablePos, cellPos } = tableInfo;
    const map = TableMap.get(tableNode);
    const cellOffset = cellPos - tablePos - 1;
    const cellIndex = map.map.indexOf(cellOffset);
    if (cellIndex < 0) return DecorationSet.empty;

    const activeCol = cellIndex % map.width;
    const activeRow = Math.floor(cellIndex / map.width);

    const decorations: Decoration[] = [];

    // Column / row decorations are always emitted — they drive the top + left handles.
    // Under a multi-cell selection they anchor to the selection's anchor cell, so the
    // column handle sits over that cell's column and the row handle sits next to its row.
    tableNode.forEach((row, rowOffset, rowIndex) => {
        const rowStart = tablePos + 1 + rowOffset;
        row.forEach((cell, cellOffsetInRow, colIndex) => {
            const isActiveCol = colIndex === activeCol;
            const isActiveRow = rowIndex === activeRow;
            if (!isActiveCol && !isActiveRow) return;

            const classes: string[] = [];
            if (isActiveCol) classes.push('is-active-column');
            if (isActiveRow) classes.push('is-active-row');

            const cellStart = rowStart + 1 + cellOffsetInRow;
            decorations.push(
                Decoration.node(cellStart, cellStart + cell.nodeSize, {
                    class: classes.join(' ')
                })
            );
        });
    });

    // Selection-anchor decoration on top: surfaces the merge / split handle for 2+ cell
    // selections. The column / row handles stay visible alongside it.
    if (isCellSelection) {
        const selectionDecoration = buildSelectionAnchorDecoration(
            state.selection as CellSelection,
            tableNode,
            tablePos,
            map
        );
        if (selectionDecoration) decorations.push(selectionDecoration);
    }

    return DecorationSet.create(state.doc, decorations);
}

/**
 * For a `CellSelection` spanning 2+ cells, return a node decoration that marks a single
 * cell in the rightmost column with `is-selection-anchor`. The chosen cell + CSS position
 * combination places the handle at the **vertical center of the selection** (not just the
 * vertical center of one cell):
 *
 *   - **Odd row count**: pick the middle row → CSS keeps the default `top: 50%` so the
 *     handle sits at the cell's center, which IS the selection's center.
 *   - **Even row count**: pick the cell at `floor(N/2)` from the top → CSS applies the
 *     `is-selection-anchor--edge` override (`top: 0`) so the handle sits at that cell's
 *     **top border**, which is the boundary between the two middle rows = the selection's
 *     vertical center.
 *
 * Returns `null` for a 1×1 selection so the regular column / row decorations stay in play.
 */
function buildSelectionAnchorDecoration(
    selection: CellSelection,
    tableNode: PMNode,
    tablePos: number,
    map: TableMap
): Decoration | null {
    const anchorOffset = selection.$anchorCell.pos - tablePos - 1;
    const headOffset = selection.$headCell.pos - tablePos - 1;
    const rect = map.rectBetween(anchorOffset, headOffset);

    // Single-cell CellSelection — fall back to the column / row decorations.
    if (rect.right - rect.left <= 1 && rect.bottom - rect.top <= 1) return null;

    const rowCount = rect.bottom - rect.top;
    const anchorRow = rect.top + Math.floor(rowCount / 2);
    const anchorCol = rect.right - 1;
    const cellIndex = anchorRow * map.width + anchorCol;
    const cellOffset = map.map[cellIndex];
    if (cellOffset == null) return null;

    const cellPos = tablePos + 1 + cellOffset;
    const cellNode = tableNode.nodeAt(cellOffset);
    if (!cellNode) return null;

    // For an even row count we'd normally use the `--edge` variant (handle at the top
    // border of the lower-half cell, which lines up with the selection's vertical center).
    // BUT when the anchor cell is **merged across that boundary** — i.e. the cell at the
    // anchor row is the same cell as the one in the row above — the "boundary" is inside
    // a single cell, not between cells. The handle at the cell's top border lands at the
    // top of the merged cell, not at its center. Detect that case and fall back to the
    // centered variant so `top: 50%` places the handle at the merged cell's visual middle
    // — which IS the selection's vertical center for a single merged-cell selection.
    const isOdd = rowCount % 2 === 1;
    const anchorCellSpansBoundary =
        !isOdd &&
        anchorRow > rect.top &&
        map.map[(anchorRow - 1) * map.width + anchorCol] === cellOffset;

    const className =
        isOdd || anchorCellSpansBoundary
            ? 'is-selection-anchor'
            : 'is-selection-anchor is-selection-anchor--edge';

    return Decoration.node(cellPos, cellPos + cellNode.nodeSize, { class: className });
}

interface TableInfo {
    tableNode: PMNode;
    tablePos: number;
    cellPos: number;
}

/** Walks up from a resolved position to find the surrounding table + cell. */
function findTableAt($pos: ResolvedPos): TableInfo | null {
    for (let depth = $pos.depth; depth > 0; depth--) {
        const node = $pos.node(depth);
        if (node.type.name === 'table') {
            const tablePos = $pos.before(depth);
            const cellDepth = depth + 2;
            if ($pos.depth < cellDepth) return null;
            const cellPos = $pos.before(cellDepth);
            return { tableNode: node, tablePos, cellPos };
        }
    }
    return null;
}

/**
 * `$anchorCell` from a `CellSelection` is resolved AT the position right before the cell —
 * its depth is the row depth, not the cell depth. Walk up from there to find the table.
 */
function findTableFromCellAnchor($anchorCell: ResolvedPos): TableInfo | null {
    for (let depth = $anchorCell.depth; depth >= 0; depth--) {
        const node = $anchorCell.node(depth);
        if (node.type.name === 'table') {
            return {
                tableNode: node,
                tablePos: $anchorCell.before(depth),
                cellPos: $anchorCell.pos
            };
        }
    }
    return null;
}
