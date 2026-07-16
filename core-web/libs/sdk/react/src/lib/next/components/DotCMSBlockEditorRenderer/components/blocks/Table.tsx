import React from 'react';

import { BlockEditorNode } from '@dotcms/types';

interface TableRendererProps {
    content: BlockEditorNode[];
    /**
     * Table-node attributes (`caption`, `ariaLabel`, `ariaLabelledby`). Optional for
     * back-compat with older payloads that don't carry these.
     */
    attrs?: BlockEditorNode['attrs'];
    blockEditorItem: React.FC<{
        content: BlockEditorNode[];
    }>;
}

/**
 * Renders a table block for the Block Editor.
 *
 * **Cell-type-aware**: each cell is emitted as `<th>` or `<td>` based on the node's
 * `type` (`tableHeader` vs `tableCell`) rather than its row position — so column-header
 * cells in any row (the result of "Toggle column header" in the editor) keep their
 * semantic `<th>` wrapper and the `scope` attribute reaches headless consumers.
 *
 * @param content - The table's child rows.
 * @param attrs - Optional table-level attributes (`caption`, `ariaLabel`, `ariaLabelledby`).
 * @param blockEditorItem - The Block Editor item component for nested content.
 */
export const TableRenderer: React.FC<TableRendererProps> = ({
    content,
    attrs,
    blockEditorItem
}: TableRendererProps) => {
    const BlockEditorItemComponent = blockEditorItem;

    const renderCellContent = (node: BlockEditorNode) => (
        <BlockEditorItemComponent content={node.content ?? []} />
    );

    const caption: string | undefined = attrs?.caption || undefined;
    const ariaLabel: string | undefined = attrs?.ariaLabel || undefined;
    const ariaLabelledBy: string | undefined = attrs?.ariaLabelledby || undefined;

    return (
        <table aria-label={ariaLabel} aria-labelledby={ariaLabelledBy}>
            {caption ? <caption>{caption}</caption> : null}
            <tbody>
                {content.map((rowNode, rowIndex) => (
                    <tr key={`row-${rowIndex}`}>
                        {rowNode.content?.map((cellNode, cellIndex) => {
                            const colSpan = Number(cellNode.attrs?.colspan || 1);
                            const rowSpan = Number(cellNode.attrs?.rowspan || 1);
                            // Cell type — not row index — decides th vs td. Matches the
                            // VTL renderer (storyblock/render.vtl).
                            if (cellNode.type === 'tableHeader') {
                                return (
                                    <th
                                        key={`cell-${cellIndex}`}
                                        colSpan={colSpan}
                                        rowSpan={rowSpan}
                                        scope={cellNode.attrs?.scope || undefined}>
                                        {renderCellContent(cellNode)}
                                    </th>
                                );
                            }
                            return (
                                <td key={`cell-${cellIndex}`} colSpan={colSpan} rowSpan={rowSpan}>
                                    {renderCellContent(cellNode)}
                                </td>
                            );
                        })}
                    </tr>
                ))}
            </tbody>
        </table>
    );
};
