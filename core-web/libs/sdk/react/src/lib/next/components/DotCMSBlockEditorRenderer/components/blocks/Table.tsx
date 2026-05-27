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
 * Renders a table component for the Block Editor.
 *
 * @param content - The content of the table (the array of `tableRow` nodes).
 * @param attrs - Optional table-level attributes (`caption`, `ariaLabel`, `ariaLabelledby`).
 * @param blockEditorItem - The Block Editor item component.
 */
export const TableRenderer: React.FC<TableRendererProps> = ({
    content,
    attrs,
    blockEditorItem
}: TableRendererProps) => {
    const BlockEditorItemComponent = blockEditorItem;

    const renderTableContent = (node: BlockEditorNode) => {
        return <BlockEditorItemComponent content={node.content ?? []} />;
    };

    const caption: string | undefined = attrs?.caption || undefined;
    const ariaLabel: string | undefined = attrs?.ariaLabel || undefined;
    const ariaLabelledBy: string | undefined = attrs?.ariaLabelledby || undefined;

    return (
        <table aria-label={ariaLabel} aria-labelledby={ariaLabelledBy}>
            {caption ? <caption>{caption}</caption> : null}
            <thead>
                {content.slice(0, 1).map((rowNode, rowIndex) => (
                    <tr key={`${rowNode.type}-${rowIndex}`}>
                        {rowNode.content?.map((cellNode, cellIndex) => (
                            <th
                                key={`${cellNode.type}-${cellIndex}`}
                                colSpan={Number(cellNode.attrs?.colspan || 1)}
                                rowSpan={Number(cellNode.attrs?.rowspan || 1)}
                                scope={cellNode.attrs?.scope || undefined}>
                                {renderTableContent(cellNode)}
                            </th>
                        ))}
                    </tr>
                ))}
            </thead>
            <tbody>
                {content.slice(1).map((rowNode, rowIndex) => (
                    <tr key={`${rowNode.type}-${rowIndex}`}>
                        {rowNode.content?.map((cellNode, cellIndex) => (
                            <td
                                key={`${cellNode.type}-${cellIndex}`}
                                colSpan={Number(cellNode.attrs?.colspan || 1)}
                                rowSpan={Number(cellNode.attrs?.rowspan || 1)}>
                                {renderTableContent(cellNode)}
                            </td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </table>
    );
};
