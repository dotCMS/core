import { mergeAttributes } from '@tiptap/core';
import { Table, TableHeader, TableKit, createColGroup } from '@tiptap/extension-table';

import { TableScopeAutoAssign } from './table-scope-auto-assign.plugin';

/**
 * Extends the upstream `Table` node so all three a11y fields (caption, aria-label,
 * aria-labelledby) live as **attributes** on the table node — symmetric storage and a
 * simple top-level shape for headless SDK consumers (`tableNode.attrs.caption`).
 *
 * The caption attribute is plain text and is rendered as a `<caption>` child element by
 * the `renderHTML` override below. It's not contenteditable from the canvas — authors set
 * it from the toolbar `table_edit` popover.
 */
const DotTable = Table.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            caption: {
                default: null,
                parseHTML: (element) => {
                    const captionEl = element.querySelector(':scope > caption');
                    return captionEl?.textContent?.trim() || null;
                },
                // Not a <table> HTML attribute — emitted as a <caption> child by the
                // node-level renderHTML override below.
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
    },

    /**
     * Mirrors `@tiptap/extension-table`'s upstream `renderHTML` (preserves the `colgroup`
     * generation that drives column resizing) and splices in a `<caption>` element when
     * `attrs.caption` is set. HTML spec ordering: `<table> > <caption>? > <colgroup>? >
     * <tbody>` — caption MUST come first inside `<table>`.
     *
     * Keep in sync with upstream if `@tiptap/extension-table` changes its renderer.
     */
    renderHTML({ node, HTMLAttributes }) {
        const { colgroup, tableWidth, tableMinWidth } = createColGroup(
            node,
            this.options.cellMinWidth
        );
        const userStyles = HTMLAttributes['style'];
        const style =
            userStyles ?? (tableWidth ? `width: ${tableWidth}` : `min-width: ${tableMinWidth}`);
        const caption = (node.attrs['caption'] as string | null)?.trim();
        const attrs = mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, { style });

        const table = caption
            ? (['table', attrs, ['caption', caption], colgroup, ['tbody', 0]] as const)
            : (['table', attrs, colgroup, ['tbody', 0]] as const);
        return this.options.renderWrapper ? ['div', { class: 'tableWrapper' }, table] : table;
    }
});

/**
 * Adds `scope` to `<th>` cells. The `TableScopeAutoAssign` ProseMirror plugin (registered
 * below) fills this attribute in based on cell position; an author can still override the
 * value to `colgroup` / `rowgroup` from the column popover — auto-assign skips non-null values.
 */
const DotTableHeader = TableHeader.extend({
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
    }
});

interface DotTableKitOptions {
    /** Forwarded to the underlying `Table` config (e.g. `{ resizable: true }`). */
    table?: Parameters<typeof Table.configure>[0];
}

/**
 * Returns the full set of table-related TipTap extensions:
 *
 *   - `DotTable` — Table extended with `caption` + `ariaLabel` + `ariaLabelledby` attributes
 *     (overrides `TableKit.table`). Caption is rendered as a `<caption>` child element.
 *   - `DotTableHeader` — TableHeader extended with `scope` (overrides `TableKit.tableHeader`).
 *   - `TableCell` + `TableRow` — supplied unchanged by `TableKit`.
 *   - `TableScopeAutoAssign` — fills `scope` on header cells based on their position.
 */
export function createDotTableExtensions(options: DotTableKitOptions = {}) {
    return [
        // The kit still provides TableRow + TableCell + ProseMirror table editing plugins.
        // We disable its table + tableHeader entries because we provide extended versions
        // below — leaving them enabled would register two nodes with the same name.
        TableKit.configure({
            table: false,
            tableHeader: false
        }),
        DotTable.configure(options.table ?? {}),
        DotTableHeader,
        TableScopeAutoAssign
    ];
}
