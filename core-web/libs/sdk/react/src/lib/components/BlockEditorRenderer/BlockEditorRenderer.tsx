import { useEffect, useRef, useState } from 'react';

import { initInlineEditing, isInsideEditor } from '@dotcms/client';

import { BlockEditorBlock } from './item/BlockEditorBlock';

import { DotCMSContentlet } from '../../models';
import { Block } from '../../models/blocks.interface';
import { CustomRenderer } from '../../models/content-node.interface';
import { isValidBlocks } from '../../utils/utils';

interface BaseProps {
    blocks: Block;
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

interface EditableProps extends BaseProps {
    editable: true;
    contentlet: DotCMSContentlet;
    fieldName: string;
}

interface NonEditableProps extends BaseProps {
    editable?: false;
    contentlet?: never;
    fieldName?: never;
}

type BlockEditorRendererProps = EditableProps | NonEditableProps;

interface BlockEditorState {
    isValid: boolean;
    error: string | null;
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
 * @param {boolean} props.editable - Flag to enable inline editing. When true, `contentlet` and `fieldName` are required. Note: Enterprise only feature.
 * @param {DotCMSContentlet} [props.contentlet] - Contentlet object for inline editing. Required when `editable` is true.
 * @param {string} [props.fieldName] - Field name for inline editing. Required when `editable` is true.
 * @returns {JSX.Element} A div containing the rendered blocks of content.
 */
export const BlockEditorRenderer = ({
    style,
    blocks,
    editable,
    fieldName,
    className,
    contentlet,
    customRenderers
}: BlockEditorRendererProps) => {
    const ref = useRef<HTMLDivElement>(null);
    const [blockEditorState, setBlockEditorState] = useState<BlockEditorState>({
        isValid: true,
        error: null
    });

    /**
     * Sets up inline editing functionality when the component is editable and inside the editor.
     *
     * This effect:
     * 1. Checks if inline editing should be enabled based on props and editor context
     * 2. Validates required props for inline editing (contentlet and fieldName)
     * 3. Extracts necessary data from the contentlet
     * 4. Adds a click handler to initialize inline editing with the block editor
     * 5. Cleans up event listener on unmount
     *
     * @dependency {boolean} editable - Flag to enable/disable inline editing
     * @dependency {DotCMSContentlet} contentlet - Contentlet data required for editing
     * @dependency {Block} blocks - The content blocks to edit
     * @dependency {string} fieldName - Name of the field being edited
     */
    useEffect(() => {
        if (!editable || !ref.current || !isInsideEditor()) {
            return;
        }

        // TypeScript will throw an error if contentlet or fieldName are not provided when editable is true,
        // but we need to check them again to avoid runtime errors in Pure JavaScript
        if (!contentlet || !fieldName) {
            console.error('contentlet and fieldName are required to enable inline editing');

            return;
        }

        const { inode, languageId: language, contentType } = contentlet;
        // `ContentNode` lives on `@dotcms/react` that's why we can use it in `@dotcms/client`
        // We need to move interfaces to external lib
        const content = blocks as unknown as Record<string, unknown>;
        const element = ref.current;
        const handleClickEvent = () => {
            initInlineEditing('BLOCK_EDITOR', {
                inode,
                content,
                language,
                fieldName,
                contentType
            });
        };

        element.addEventListener('click', handleClickEvent);

        return () => element.removeEventListener('click', handleClickEvent);
    }, [editable, contentlet, blocks, fieldName]);

    /**
     * Validates the blocks prop and updates the BlockEditorState accordingly.
     * If blocks is valid, clears any error state.
     * If blocks is invalid, sets error state and logs error message.
     */
    useEffect(() => {
        if (isValidBlocks(blocks)) {
            setBlockEditorState({
                isValid: true,
                error: null
            });
        } else {
            setBlockEditorState({
                isValid: false,
                error: 'BlockEditorRenderer Error: Invalid prop "blocks"'
            });
            console.error('BlockEditorRenderer Error: Invalid prop "blocks"');
        }
    }, [blocks]);

    if (!blockEditorState.isValid) {
        console.error(blockEditorState.error);
        if (isInsideEditor()) {
            return <div key="invalid-blocks-message">{blockEditorState.error}</div>;
        }

        return null;
    }

    return (
        <div className={className} style={style} ref={ref} data-testid="dot-block-editor-container">
            <BlockEditorBlock content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
