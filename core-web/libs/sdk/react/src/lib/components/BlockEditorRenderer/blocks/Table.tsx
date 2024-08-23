import React from 'react';

import {  ContentNode } from '../BlockEditorRenderer';

interface TableRendererProps {
    content: ContentNode[];
    blockEditorItem: React.FC<{
        content: ContentNode[];
    }>;
}

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
                {content.slice(0, 1).map((rowNode, rowIndex) => (
                    <tr key={rowIndex}>
                        {rowNode.content?.map((cellNode, cellIndex) => (
                            <th
                                key={cellIndex}
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
                    <tr key={rowIndex}>
                        {rowNode.content?.map((cellNode, cellIndex) => (
                            <td
                                key={cellIndex}
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
