import { useEffect, useState } from 'react';

import { BlockEditorContent } from '@dotcms/types';
import { BlockEditorState } from '@dotcms/types/internal';
import { isValidBlocks } from '@dotcms/uve/internal';

import { BlockEditorBlock } from './components/BlockEditorBlock';

import { useIsDevMode } from '../../hooks/useIsDevMode';

/**
 * Represents a Custom Renderer used by the Block Editor Component
 *
 * @export
 * @interface CustomRenderer
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRenderer<T = any> = Record<string, React.FC<T>>;

export interface BlockEditorRendererProps {
    blocks: BlockEditorContent;
    style?: React.CSSProperties;
    className?: string;
    customRenderers?: CustomRenderer;
}

/**
 * BlockEditorRenderer component for rendering block editor field.
 *
 * @component
 * @param {Object} props - The component props.
 * @param {BlockEditorContent} props.blocks - The blocks of content to render.
 * @param {CustomRenderer} [props.customRenderers] - Optional custom renderers for specific block types.
 * @param {string} [props.className] - Optional CSS class name for the container div.
 * @param {React.CSSProperties} [props.style] - Optional inline styles for the container div.
 * @returns {JSX.Element} A div containing the rendered blocks of content.
 */
export const DotCMSBlockEditorRenderer = ({
    blocks,
    style,
    className,
    customRenderers
}: BlockEditorRendererProps) => {
    const [blockEditorState, setBlockEditorState] = useState<BlockEditorState>({ error: null });
    const isDevMode = useIsDevMode();

    /**
     * Validates the blocks structure and updates the block editor state.
     *
     * This effect:
     * 1. Validates that blocks have the correct structure (doc type, content array, etc)
     * 2. Updates the block editor state with validation result
     * 3. Logs any validation errors to console
     *
     * @dependency {Block} blocks - The content blocks to validate
     */
    useEffect(() => {
        const validationResult = isValidBlocks(blocks);
        setBlockEditorState(validationResult);

        if (validationResult.error) {
            console.error(validationResult.error);
        }
    }, [blocks]);

    if (blockEditorState.error) {
        console.error(blockEditorState.error);

        if (isDevMode) {
            return <div data-testid="invalid-blocks-message">{blockEditorState.error}</div>;
        }

        return null;
    }

    return (
        <div className={className} style={style} data-testid="dot-block-editor-container">
            <BlockEditorBlock content={blocks?.content} customRenderers={customRenderers} />
        </div>
    );
};
