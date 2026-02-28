import { BlockEditorNode } from '@dotcms/types';

import { CustomRenderer } from '../../DotCMSBlockEditorRenderer';

interface GridBlockProps {
    node: BlockEditorNode;
    children: React.ReactNode;
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
    const cols = Array.isArray(node.attrs?.columns) ? node.attrs.columns : [6, 6];
    const pct1 = (cols[0] / 12) * 100;
    const pct2 = (cols[1] / 12) * 100;

    return (
        <div
            data-type="gridBlock"
            className="grid-block"
            style={
                {
                    display: 'grid',
                    gridTemplateColumns: `${pct1}% ${pct2}%`,
                    gap: '1rem'
                } as React.CSSProperties
            }>
            {node.content?.map((column, index) => (
                <div
                    key={`gridColumn-${index}`}
                    data-type="gridColumn"
                    className="grid-block__column">
                    <BlockEditorBlockComponent
                        content={column.content}
                        customRenderers={customRenderers}
                    />
                </div>
            ))}
        </div>
    );
};
