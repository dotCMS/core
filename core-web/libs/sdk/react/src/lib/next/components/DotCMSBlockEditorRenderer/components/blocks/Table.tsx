import React from 'react';

import { BlockEditorNode } from '@dotcms/types';

interface TableRendererProps {
    content: BlockEditorNode[];
    blockEditorItem: React.FC<{
        content: BlockEditorNode[];
    }>;
}

/**
 * Renders a table component for the Block Editor.
 *
 * @param content - The content of the table.
 * @param blockEditorItem - The Block Editor item component.
 */
export const TableRenderer: React.FC<TableRendererProps> = ({
    content,
    blockEditorItem
}: TableRendererProps) => {
    const BlockEditorItemComponent = blockEditorItem;

    const renderTableContent = (node: BlockEditorNode) => {
        return <BlockEditorItemComponent content={node.content ?? []} />;
    };

    return (
        <table>
            <thead>
                {content.slice(0, 1).map((rowNode, rowIndex) => (
                    <tr key={`${rowNode.type}-${rowIndex}`}>
                        {rowNode.content?.map((cellNode, cellIndex) => (
                            <th
                                key={`${cellNode.type}-${cellIndex}`}
                                colSpan={Number(cellNode.attrs?.colspan || 1)}
                                rowSpan={Number(cellNode.attrs?.rowspan || 1)}>
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
