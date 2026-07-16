import { BlockEditorNode } from '@dotcms/types';
import { isValidBlocks } from '@dotcms/uve/internal';

import { BlockEditorBlock } from './components/BlockEditorBlock';

/**
 * Props that all custom renderers must accept.
 *
 * @export
 * @interface CustomRendererProps
 * @template TData - The type of data stored in node.attrs.data (for contentlet blocks)
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface CustomRendererProps<TData = any> {
    /** The full BlockEditorNode with attrs, marks, content, etc. */
    node: BlockEditorNode & {
        attrs?: {
            data?: TData;
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            [key: string]: any;
        };
    };
    /** Rendered children from nested content (if any) */
    children?: React.ReactNode;
}

/**
 * Custom renderer component type - must accept node and optional children.
 * Can be specialized with a specific data type for node.attrs.data.
 *
 * @export
 * @template TData - The type of contentlet data in node.attrs.data
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRendererComponent<TData = any> = React.FC<CustomRendererProps<TData>>;

/**
 * Map of block type names to custom renderer components.
 * Use the generic parameter to type specific contentlet data.
 *
 * @export
 * @interface CustomRenderer
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRenderer = Record<string, CustomRendererComponent<any>>;

export interface BlockEditorRendererProps {
    blocks: BlockEditorNode;
    style?: React.CSSProperties;
    className?: string;
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}

/**
 * BlockEditorRenderer component for rendering block editor field.
 *
 * @component
 * @param {Object} props - The component props.
 * @param {BlockEditorNode} props.blocks - The blocks of content to render.
 * @param {CustomRenderer} [props.customRenderers] - Optional custom renderers for specific block types.
 * @param {string} [props.className] - Optional CSS class name for the container div.
 * @param {React.CSSProperties} [props.style] - Optional inline styles for the container div.
 * @returns {JSX.Element} A div containing the rendered blocks of content.
 */
export const DotCMSBlockEditorRenderer = ({
    blocks,
    style,
    className,
    customRenderers,
    isDevMode = false
}: BlockEditorRendererProps) => {
    const validationResult = isValidBlocks(blocks);

    if (validationResult.error) {
        console.error(validationResult.error);

        if (isDevMode) {
            return <div data-testid="invalid-blocks-message">{validationResult.error}</div>;
        }

        return null;
    }

    return (
        <div className={className} style={style} data-testid="dot-block-editor-container">
            <BlockEditorBlock
                content={blocks?.content}
                customRenderers={customRenderers}
                isDevMode={isDevMode}
            />
        </div>
    );
};
