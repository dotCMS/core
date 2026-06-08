import { NodeViewRenderer } from '@tiptap/core';
import { Table, TableCell, TableHeader, TableKit } from '@tiptap/extension-table';
import { Node as PMNode } from '@tiptap/pm/model';
import { toggleHeaderColumn, toggleHeaderRow } from '@tiptap/pm/tables';

import { TableScopeAutoAssign } from './table-scope-auto-assign.plugin';

import type { EditorPopoverService } from '../services/editor-popover.service';

// ── DotTable ───────────────────────────────────────────────────────────────────────

const DotTable = Table.extend({
    /**
     * TipTap's built-in `toggleHeaderRow` / `toggleHeaderColumn` call prosemirror-tables'
     * `toggleHeader('row' | 'column')` with its modern logic, which is hard-coded to the
     * FIRST row / FIRST column regardless of the cursor position (it builds the cell rect with
     * `top: 0, bottom: 1` for rows and `left: 0, right: 1` for columns). dotCMS exposes a
     * per-row / per-column header toggle through the cell handle popovers, so the default
     * commands make every toggle land on the first row/column only.
     *
     * We override them with prosemirror-tables' deprecated variants, which operate on the row /
     * column of the *currently selected* cell (the popover places the selection in the target
     * cell before running the command). See issue #35980 (bug 3).
     */
    addCommands() {
        return {
            ...this.parent?.(),
            toggleHeaderRow:
                () =>
                ({ state, dispatch }) =>
                    toggleHeaderRow(state, dispatch),
            toggleHeaderColumn:
                () =>
                ({ state, dispatch }) =>
                    toggleHeaderColumn(state, dispatch)
        };
    },
    addAttributes() {
        return {
            ...this.parent?.(),
            caption: {
                default: null,
                parseHTML: (element) => {
                    const captionEl = element.querySelector(':scope > caption');
                    return captionEl?.textContent?.trim() || null;
                },
                renderHTML: () => ({})
            },
            ariaLabel: {
                default: null,
                parseHTML: (element) => element.getAttribute('aria-label'),
                renderHTML: (attributes) => {
                    const value = attributes['ariaLabel'];
                    if (value == null || value === '') return {};
                    return { 'aria-label': value };
                }
            },
            ariaLabelledby: {
                default: null,
                parseHTML: (element) => element.getAttribute('aria-labelledby'),
                renderHTML: (attributes) => {
                    const value = attributes['ariaLabelledby'];
                    if (value == null || value === '') return {};
                    return { 'aria-labelledby': value };
                }
            }
        };
    }
});

// ── DotTableCell / DotTableHeader (NodeViews with embedded handles) ────────────────

interface CellExtensionOptions {
    HTMLAttributes: Record<string, unknown>;
    /** Injected by the editor component scope so click handlers can open the scoped popovers. */
    popovers: EditorPopoverService | null;
    /** i18n labels for the handle buttons; supplied at extension-construction time. */
    columnAriaLabel: string;
    rowAriaLabel: string;
    selectionAriaLabel: string;
}

const CELL_ATTRS_TO_SYNC = ['colspan', 'rowspan', 'colwidth', 'align', 'scope'] as const;

/**
 * Renders a `<td>` / `<th>` with two child buttons (column handle + row handle) plus a
 * content container. The buttons live inside the cell DOM, so positioning is pure CSS:
 *
 *   - `--col` → `top: -12px; left: 50%; transform: translateX(-50%)`
 *   - `--row` → `top: 50%; left: -12px; transform: translateY(-50%)`
 *
 * The {@link TableActiveCellsPlugin} adds `.is-active-column` / `.is-active-row` classes to
 * cells in the cursor's column / row; CSS first-child selectors then show the handle only on
 * the first-row cell of the active column (and the first-cell of the active row).
 */
function makeCellNodeViewFactory(
    tag: 'td' | 'th',
    options: CellExtensionOptions
): NodeViewRenderer {
    return ({ node, getPos, HTMLAttributes }) => {
        const popovers = options.popovers;
        const cell = document.createElement(tag);
        applyHTMLAttributes(cell, HTMLAttributes);

        const colHandle = makeHandleButton('column', 'more_horiz', options.columnAriaLabel);
        const rowHandle = makeHandleButton('row', 'more_vert', options.rowAriaLabel);
        const selectionHandle = makeHandleButton(
            'selection',
            'drag_indicator',
            options.selectionAriaLabel
        );

        const content = document.createElement('div');
        content.className = 'dot-cell-content';

        cell.append(colHandle, rowHandle, selectionHandle, content);

        const resolveCellPos = (): number | null => {
            const pos = typeof getPos === 'function' ? getPos() : null;
            return typeof pos === 'number' ? pos : null;
        };

        if (popovers) {
            colHandle.addEventListener('mousedown', (event) => {
                event.preventDefault();
                event.stopPropagation();
                const pos = resolveCellPos();
                if (pos == null) return;
                popovers.openTableColumn(() => colHandle.getBoundingClientRect(), {
                    cellPos: pos,
                    isHeader: tag === 'th',
                    headerScope: (currentNode.attrs['scope'] as string | null) ?? ''
                });
            });
            rowHandle.addEventListener('mousedown', (event) => {
                event.preventDefault();
                event.stopPropagation();
                const pos = resolveCellPos();
                if (pos == null) return;
                popovers.openTableRow(() => rowHandle.getBoundingClientRect(), {
                    cellPos: pos
                });
            });
            // The selection handle is only visible when this cell carries the
            // `is-selection-anchor` decoration (multi-cell CellSelection). The CellSelection
            // itself stays alive across the click via mousedown.preventDefault, so the merge
            // / split commands invoked from the popover see the right selection.
            selectionHandle.addEventListener('mousedown', (event) => {
                event.preventDefault();
                event.stopPropagation();
                popovers.openTableSelection(() => selectionHandle.getBoundingClientRect());
            });
        }

        // The closure captures `node` at creation time. We update this reference in
        // `update()` so the click handlers always see the latest attrs (e.g. scope).
        let currentNode: PMNode = node;

        return {
            dom: cell,
            contentDOM: content,
            update: (newNode) => {
                if (newNode.type.name !== node.type.name) return false;
                currentNode = newNode;
                // Sync the known attrs onto the cell element. We can't just call
                // `applyHTMLAttributes` again because the new HTMLAttributes aren't
                // passed to `update` — we resolve from `newNode.attrs` directly.
                for (const attr of CELL_ATTRS_TO_SYNC) {
                    const value = newNode.attrs[attr];
                    if (value == null || value === '') cell.removeAttribute(attr);
                    else cell.setAttribute(attr, String(value));
                }
                return true;
            },
            // The handle buttons + their icon spans are NodeView-owned DOM. Prevent
            // ProseMirror from re-parsing them on every mutation inside.
            ignoreMutation: (mutation) => {
                const target = mutation.target as Element | null;
                if (!target) return false;
                if (target instanceof HTMLElement && target.closest('.dot-cell-handle')) {
                    return true;
                }
                return false;
            }
        };
    };
}

function makeHandleButton(
    kind: 'column' | 'row' | 'selection',
    icon: string,
    ariaLabel: string
): HTMLButtonElement {
    const button = document.createElement('button');
    button.type = 'button';
    const modifier = kind === 'column' ? 'col' : kind === 'row' ? 'row' : 'selection';
    button.className = `dot-cell-handle dot-cell-handle--${modifier}`;
    button.setAttribute('contenteditable', 'false');
    button.setAttribute('tabindex', '-1');
    button.setAttribute('aria-label', ariaLabel);
    button.dataset['testid'] = `table-${kind}-handle`;

    const iconSpan = document.createElement('span');
    iconSpan.className = 'material-symbols-outlined';
    iconSpan.setAttribute('aria-hidden', 'true');
    iconSpan.textContent = icon;
    button.appendChild(iconSpan);
    return button;
}

function applyHTMLAttributes(el: HTMLElement, attrs: Record<string, unknown>): void {
    for (const [key, value] of Object.entries(attrs)) {
        if (value == null || value === '' || value === false) continue;
        el.setAttribute(key, String(value));
    }
}

const DotTableCell = TableCell.extend<CellExtensionOptions>({
    addOptions() {
        return {
            ...this.parent?.(),
            popovers: null,
            columnAriaLabel: 'Column actions',
            rowAriaLabel: 'Row actions',
            selectionAriaLabel: 'Selection actions'
        };
    },
    addNodeView() {
        return makeCellNodeViewFactory('td', this.options);
    }
});

const DotTableHeader = TableHeader.extend<CellExtensionOptions>({
    addOptions() {
        return {
            ...this.parent?.(),
            popovers: null,
            columnAriaLabel: 'Column actions',
            rowAriaLabel: 'Row actions',
            selectionAriaLabel: 'Selection actions'
        };
    },
    addAttributes() {
        return {
            ...this.parent?.(),
            scope: {
                default: null,
                parseHTML: (element) => element.getAttribute('scope'),
                renderHTML: (attributes) => {
                    const value = attributes['scope'];
                    if (value == null || value === '') return {};
                    return { scope: value };
                }
            }
        };
    },
    addNodeView() {
        return makeCellNodeViewFactory('th', this.options);
    }
});

// ── Bundle ─────────────────────────────────────────────────────────────────────────

interface DotTableKitOptions {
    table?: Parameters<typeof Table.configure>[0];
    /** Cell + header NodeView options — used to inject the popover service + aria labels. */
    cell?: Partial<CellExtensionOptions>;
    header?: Partial<CellExtensionOptions>;
}

/**
 * Returns the full set of table-related TipTap extensions. The cell + header NodeViews
 * each receive an {@link EditorPopoverService} via options so their click handlers can open
 * the column / row popovers without going through Angular DI.
 *
 *   - `DotTable` — adds caption + aria-label + aria-labelledby attributes.
 *   - `DotTableCell` / `DotTableHeader` — NodeView renders handle buttons inside the cell.
 *   - `TableCell` + `TableRow` come from `TableKit` (cell is overridden here; row stays default).
 *   - `TableScopeAutoAssign` — fills `scope` on header cells based on position.
 */
export function createDotTableExtensions(options: DotTableKitOptions = {}) {
    return [
        // We provide custom Table, TableCell and TableHeader; disable the kit's versions.
        TableKit.configure({
            table: false,
            tableCell: false,
            tableHeader: false
        }),
        DotTable.configure(options.table ?? {}),
        DotTableCell.configure(options.cell ?? {}),
        DotTableHeader.configure(options.header ?? {}),
        TableScopeAutoAssign
    ];
}
