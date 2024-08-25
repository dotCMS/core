import { BlockEditorItem } from './item/BlockEditorItem';

import { Block, CustomRenderer } from '../../models/blocks.interface';

interface BlockEditorRendererProps {
    blocks: Block;
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

/**
 * Renders a block editor with the specified blocks, custom renderers, className, and style.
 *
 * @param blocks - The blocks to be rendered in the editor.
 * @param customRenderers - Custom renderers for specific block types.
 * @param className - The CSS class name for the container element.
 * @param style - The inline styles for the container element.
 * @returns The rendered block editor.
 */
export const BlockEditorRenderer = ({
    blocks,
    customRenderers,
    className,
    style
}: BlockEditorRendererProps) => {
    return (
        <div className={className} style={style}>
            <BlockEditorItem content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
