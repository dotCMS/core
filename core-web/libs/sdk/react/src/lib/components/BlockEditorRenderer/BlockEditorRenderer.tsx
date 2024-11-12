import { useEffect, useRef } from 'react';

import { initInlineEditing } from '@dotcms/client';

import { BlockEditorBlock } from './item/BlockEditorBlock';

import { DotCMSContentlet } from '../../models';
import { Block } from '../../models/blocks.interface';
import { CustomRenderer } from '../../models/content-node.interface';

export interface BlockEditorRendererProps {
    blocks: Block;
    editable?: boolean;
    contentlet?: DotCMSContentlet;
    fieldName?: string;
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
    editable,
    contentlet,
    fieldName,
    customRenderers,
    className,
    style
}: BlockEditorRendererProps) => {
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!editable || !ref.current) {
            return;
        }

        ref.current?.addEventListener('click', () => {
            initInlineEditing('blockEditor', { ...contentlet, fieldName, content: blocks.content });
        });
    }, [editable, contentlet, blocks.content, fieldName]);

    return (
        <div className={className} style={style} ref={ref}>
            <BlockEditorBlock content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
