import { BlockEditorMark, BlockEditorNode } from '@dotcms/types';

interface MarkProps extends BlockEditorMark {
    children: React.ReactNode;
}

interface TextComponentProp {
    children: React.ReactNode;
    node: BlockEditorNode;
}

interface TextNodeProps {
    marks?: BlockEditorMark[];
    text?: string;
}

/**
 * Renders the text in bold.
 *
 * @param children - The content to be rendered in bold.
 */
export const Bold = ({ children }: MarkProps) => <strong>{children}</strong>;

/**
 * Renders the text in italic format.
 *
 * @param children - The content to be rendered in italic.
 */
export const Italic = ({ children }: MarkProps) => <em>{children}</em>;

/**
 * Renders a strike-through text.
 *
 * @param children - The content to be rendered within the strike-through element.
 */
export const Strike = ({ children }: MarkProps) => <s>{children}</s>;

/**
 * Renders an underline element for the given children.
 *
 * @param children - The content to be underlined.
 */
export const Underline = ({ children }: MarkProps) => <u>{children}</u>;

/**
 * Renders a paragraph element.
 *
 * @param children - The content of the paragraph.
 * @param attrs - The style attributes for the paragraph.
 * @returns The rendered paragraph element.
 */
export const Paragraph = ({ children, node }: TextComponentProp) => {
    const attrs = node?.attrs || {};

    return <p style={attrs}>{children}</p>;
};

/**
 * Renders a link component.
 *
 * @param children - The content of the link.
 * @param attrs - The attributes to be applied to the link.
 * @returns The rendered link component.
 */
export const Link = ({ children, attrs }: MarkProps) => {
    return <a {...attrs}>{children}</a>;
};

/**
 * Renders a heading element with the specified level.
 *
 * @param children - The content of the heading.
 * @param attrs - The attributes for the heading.
 * @returns The rendered heading element.
 */
export const Heading = ({ children, node }: TextComponentProp) => {
    const attrs = node?.attrs || {};
    const level = attrs.level || 1;
    const Tag = `h${level}` as keyof JSX.IntrinsicElements;

    return <Tag>{children}</Tag>;
};

/**
 * Renders the superscript text.
 *
 * @param children - The content to be rendered as superscript.
 */
export const Superscript = ({ children }: MarkProps) => <sup>{children}</sup>;

/**
 * Renders a subscript element.
 *
 * @param children - The content to be rendered as subscript.
 */
export const Subscript = ({ children }: MarkProps) => <sub>{children}</sub>;

const nodeMarks: Record<string, React.FC<MarkProps>> = {
    bold: Bold,
    link: Link,
    italic: Italic,
    strike: Strike,
    subscript: Subscript,
    underline: Underline,
    superscript: Superscript
};

const defaultMark: BlockEditorMark = { type: '', attrs: {} };

/**
 * Renders a text block with optional marks.
 *
 * @param props - The props for the TextBlock component.
 * @returns The rendered text block.
 */
export const TextBlock = (props: TextNodeProps = {}) => {
    const { marks = [], text } = props;
    const mark = marks[0] || defaultMark;
    const textProps = { ...props, marks: marks.slice(1) };
    const Component = nodeMarks[mark?.type];

    // In React, class is not a valid attribute name, so we need to rename it to className
    if (mark.attrs) {
        mark.attrs.className = mark.attrs.class;
        delete mark.attrs.class;
    }

    if (!Component) {
        return text;
    }

    return (
        <Component type={mark.type} attrs={mark.attrs}>
            <TextBlock {...textProps} />
        </Component>
    );
};
