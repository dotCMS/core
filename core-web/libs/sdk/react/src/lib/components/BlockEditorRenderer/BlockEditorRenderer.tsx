import { BlockEditorBlock } from './item/BlockEditorBlock';

import { Block } from '../../models/blocks.interface';
import { CustomRenderer } from '../../models/content-node.interface';

export interface BlockEditorRendererProps {
    blocks: Block;
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

/**
 * BlockEditorRenderer component for rendering block editor field.
 *
 * @component
 * @param {Object} props - The component props.
 * @param {Block} props.blocks - The blocks of content to render.
 * @param {CustomRenderer} [props.customRenderers] - Optional custom renderers for specific block types.
 * @param {string} [props.className] - Optional CSS class name for the container div.
 * @param {React.CSSProperties} [props.style] - Optional inline styles for the container div.
 * @returns {JSX.Element} A div containing the rendered blocks of content.
 */
export const BlockEditorRenderer = ({
    blocks,
    customRenderers,
    className,
    style
}: BlockEditorRendererProps) => {
    return (
        <div className={className} style={style}>
            <BlockEditorBlock content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
