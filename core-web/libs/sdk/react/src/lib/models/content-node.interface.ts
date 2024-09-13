import { BlockProps } from './blocks.interface';

/**
 * Represents a Mark used by text content in the Block Editor
 *
 * @export
 * @interface Mark
 */
export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

/**
 * Represents a Content Node used by the Block Editor
 *
 * @export
 * @interface ContentNode
 */
export interface ContentNode {
    type: string;
    content: ContentNode[];
    attrs?: Record<string, string>;
    marks?: Mark[];
    text?: string;
}

/**
 * Represents a Custom Renderer used by the Block Editor Component
 *
 * @export
 * @interface CustomRenderer
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRenderer<T = any> = Record<string, React.FC<T>>;

/**
 * Represents a CodeBlock used by the Block Editor Component
 * @export
 * @interface CodeBlockProps
 */
export type CodeBlockProps = BlockProps & ContentNode;

/**
 * Represents a Heading used by the Block Editor Component
 * @export
 * @interface HeadingProps
 */
export type HeadingProps = BlockProps & ContentNode;

/**
 * Represents a Link used by the Block Editor Component
 * @export
 * @interface LinkProps
 */
export type LinkProps = BlockProps & { attrs?: Mark['attrs'] };

/**
 * Represents a Paragraph used by the Block Editor Component
 * @export
 * @interface ParagraphProps
 */
export type ParagraphProps = BlockProps & ContentNode;

/**
 * Represents a DotCMSVideo used by the Block Editor Component
 * @export
 * @interface DotCMSVideoProps
 */
export type DotCMSVideoProps = ContentNode['attrs'] & {
    data?: Record<string, string>;
};

/**
 * Represents a DotCMSImage used by the Block Editor Component
 * @export
 * @interface DotCMSImageProps
 */
export type DotCMSImageProps = ContentNode['attrs'] & {
    data?: Record<string, unknown>;
};

/**
 * Represents a DotContent used by the Block Editor Component
 * @export
 * @interface DotContentProps
 */
export type DotContentProps = ContentNode & {
    customRenderers?: CustomRenderer;
};
