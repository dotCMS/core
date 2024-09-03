import { BlockProps } from './blocks.interface';

export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export interface ContentNode {
    type: string;
    content: ContentNode[];
    attrs?: Record<string, string>;
    marks?: Mark[];
    text?: string;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRenderer<T = any> = Record<string, React.FC<T>>;

export type CodeBlockProps = BlockProps & ContentNode;

export type HeadingProps = BlockProps & ContentNode;

export type LinkProps = BlockProps & { attrs?: Mark['attrs'] };

export type ParagraphProps = BlockProps & ContentNode;

export type DotCMSVideoProps = ContentNode['attrs'] & {
    data?: Record<string, string>;
};

export type DotCMSImageProps = ContentNode['attrs'] & {
    data?: Record<string, unknown>;
};

export type DotContentProps = ContentNode & {
    customRenderers?: CustomRenderer;
};
