import { Extension } from '@tiptap/core';
import { Node as PMNode, ResolvedPos } from '@tiptap/pm/model';
import { EditorState, Plugin, PluginKey } from '@tiptap/pm/state';
import { TableMap } from '@tiptap/pm/tables';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

const PLUGIN_KEY = new PluginKey<DecorationSet>('tableActiveCells');

/**
 * Marks the cells in the cursor's column and row with CSS classes so the
 * {@link DotTableCell} / {@link DotTableHeader} NodeViews can show the column / row handles
 * only on the right cells (first-row of active column for the column handle, first-cell of
 * active row for the row handle).
 *
 * Decorations applied per cell:
 *   - `is-active-column` — every cell whose column matches the cursor's column
 *   - `is-active-row` — every cell whose row matches the cursor's row
 *
 * No floating-ui, no autoUpdate, no store. CSS does the rest:
 *
 *   ```css
 *   .tiptap tbody > tr:first-child > .is-active-column > .dot-cell-handle--col { display: inline-flex; }
 *   .tiptap tbody > tr > .is-active-row:first-child > .dot-cell-handle--row { display: inline-flex; }
 *   ```
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
    const tableInfo = findTableAt(state.selection.$from);
    if (!tableInfo) return DecorationSet.empty;

    const { tableNode, tablePos, cellPos } = tableInfo;
    const map = TableMap.get(tableNode);
    const cellOffset = cellPos - tablePos - 1;
    const cellIndex = map.map.indexOf(cellOffset);
    if (cellIndex < 0) return DecorationSet.empty;

    const activeCol = cellIndex % map.width;
    const activeRow = Math.floor(cellIndex / map.width);

    const decorations: Decoration[] = [];

    // Walk every cell in the table once, deciding which class set applies.
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

    return DecorationSet.create(state.doc, decorations);
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
