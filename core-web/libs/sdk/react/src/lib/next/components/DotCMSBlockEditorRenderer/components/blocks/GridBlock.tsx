import { BlockEditorNode } from '@dotcms/types';

import { CustomRenderer } from '../../DotCMSBlockEditorRenderer';

interface GridBlockProps {
    node: BlockEditorNode;
    customRenderers?: CustomRenderer;
    blockEditorBlock: React.FC<{
        content: BlockEditorNode[] | undefined;
        customRenderers?: CustomRenderer;
    }>;
}

/**
 * Renders a grid block with two columns using a 12-column grid system.
 *
 * @param node - The grid block node containing column configuration.
 * @param blockEditorBlock - The block editor component for rendering nested content.
 * @param customRenderers - Optional custom renderers for nested blocks.
 */
export const GridBlock = ({ node, blockEditorBlock, customRenderers }: GridBlockProps) => {
    const BlockEditorBlockComponent = blockEditorBlock;
    const rawCols = Array.isArray(node.attrs?.columns) ? node.attrs.columns : [6, 6];
    const cols =
        rawCols.length === 2 &&
        rawCols.every((v: unknown) => typeof v === 'number' && Number.isFinite(v))
            ? rawCols
            : [6, 6];

    return (
        <div
            data-type="gridBlock"
            className="grid-block"
            style={
                {
                    display: 'grid',
                    gridTemplateColumns: 'repeat(12, 1fr)',
                    gap: '1rem'
                } as React.CSSProperties
            }>
            {node.content?.map((column, index) => (
                <div
                    key={`gridColumn-${index}`}
                    data-type="gridColumn"
                    className="grid-block__column"
                    style={{ gridColumn: `span ${cols[index] ?? 6}` }}>
                    <BlockEditorBlockComponent
                        content={column.content}
                        customRenderers={customRenderers}
                    />
                </div>
            ))}
        </div>
    );
};
