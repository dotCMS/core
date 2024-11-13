import { useEffect, useRef } from 'react';

import { initInlineEditing, isInsideEditor } from '@dotcms/client';

import { BlockEditorBlock } from './item/BlockEditorBlock';

import { DotCMSContentlet } from '../../models';
import { Block } from '../../models/blocks.interface';
import { CustomRenderer } from '../../models/content-node.interface';

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

        const { inode, languageId, contentType } = contentlet;
        const content = blocks as unknown as Record<string, unknown>;
        const element = ref.current;
        const handleClickEvent = () => {
            initInlineEditing('block-editor', {
                inode,
                languageId,
                contentType,
                fieldName,
                content
            });
        };

        element.addEventListener('click', handleClickEvent);

        return () => element.removeEventListener('click', handleClickEvent);
    }, [editable, contentlet, blocks, fieldName]);

    return (
        <div className={className} style={style} ref={ref} data-testid="dot-block-editor-container">
            <BlockEditorBlock content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
