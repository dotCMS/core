import React from 'react';

import { ContentNode } from '../../../models/blocks.interface';

interface TableRendererProps {
    content: ContentNode[];
    blockEditorItem: React.FC<{
        content: ContentNode[];
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

    const renderTableContent = (node: ContentNode) => {
        return <BlockEditorItemComponent content={node.content} />;
    };

    return (
        <table>
            <thead>
                {content.slice(0, 1).map((rowNode) => (
                    <tr key={`${rowNode.type}-${Math.random()}`}>
                        {rowNode.content?.map((cellNode) => (
                            <th
                                key={`${cellNode.type}-${Math.random()}`}
                                colSpan={Number(cellNode.attrs?.colspan || 1)}
                                rowSpan={Number(cellNode.attrs?.rowspan || 1)}>
                                {renderTableContent(cellNode)}
                            </th>
                        ))}
                    </tr>
                ))}
            </thead>
            <tbody>
                {content.slice(1).map((rowNode) => (
                    <tr key={`${rowNode.type}-${Math.random()}`}>
                        {rowNode.content?.map((cellNode) => (
                            <td
                                key={`${cellNode.type}-${Math.random()}`}
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
