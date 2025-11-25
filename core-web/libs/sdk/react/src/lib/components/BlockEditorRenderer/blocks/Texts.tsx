import { BlockProps } from '../../../models/blocks.interface';
import {
    ContentNode,
    HeadingProps,
    LinkProps,
    ParagraphProps
} from '../../../models/content-node.interface';

/**
 * Renders the text in bold.
 *
 * @param children - The content to be rendered in bold.
 */
export const Bold = ({ children }: BlockProps) => <strong>{children}</strong>;

/**
 * Renders the text in italic format.
 *
 * @param children - The content to be rendered in italic.
 */
export const Italic = ({ children }: BlockProps) => <em>{children}</em>;

/**
 * Renders a strike-through text.
 *
 * @param children - The content to be rendered within the strike-through element.
 */
export const Strike = ({ children }: BlockProps) => <s>{children}</s>;

/**
 * Renders an underline element for the given children.
 *
 * @param children - The content to be underlined.
 */
export const Underline = ({ children }: BlockProps) => <u>{children}</u>;

/**
 * Renders a paragraph element.
 *
 * @param children - The content of the paragraph.
 * @param attrs - The style attributes for the paragraph.
 * @returns The rendered paragraph element.
 */
export const Paragraph = ({ children, attrs }: ParagraphProps) => {
    return <p style={attrs}>{children}</p>;
};

/**
 * Renders a link component.
 *
 * @param children - The content of the link.
 * @param attrs - The attributes to be applied to the link.
 * @returns The rendered link component.
 */
export const Link = ({ children, attrs }: LinkProps) => {
    return <a {...attrs}>{children}</a>;
};

/**
 * Renders a heading element with the specified level.
 *
 * @param children - The content of the heading.
 * @param attrs - The attributes for the heading.
 * @returns The rendered heading element.
 */
export const Heading = ({ children, attrs }: HeadingProps) => {
    const level = attrs?.level || 1;
    const Tag = `h${level}` as keyof JSX.IntrinsicElements;

    return <Tag>{children}</Tag>;
};

/**
 * Renders the superscript text.
 *
 * @param children - The content to be rendered as superscript.
 */
export const Superscript = ({ children }: BlockProps) => <sup>{children}</sup>;

/**
 * Renders a subscript element.
 *
 * @param children - The content to be rendered as subscript.
 */
export const Subscript = ({ children }: BlockProps) => <sub>{children}</sub>;

const nodeMarks: Record<string, React.FC<BlockProps | LinkProps | HeadingProps>> = {
    link: Link,
    bold: Bold,
    underline: Underline,
    italic: Italic,
    strike: Strike,
    superscript: Superscript,
    subscript: Subscript
};

type TextBlockProps = Omit<ContentNode, 'content' | 'attrs'>;

/**
 * Renders a text block with optional marks.
 *
 * @param props - The props for the TextBlock component.
 * @returns The rendered text block.
 */
export const TextBlock = (props: TextBlockProps) => {
    const { marks = [], text } = props;
    const mark = marks[0] || { type: '', attrs: {} };
    const newProps = { ...props, marks: marks.slice(1) };
    const Component = nodeMarks[mark?.type];

    // To avoid the warning: "Warning: Invalid DOM property `class`. Did you mean `className`?"
    if (mark.attrs) {
        mark.attrs.className = mark.attrs.class;
        delete mark.attrs.class;
    }

    if (!Component) {
        return text;
    }

    return (
        <Component attrs={mark.attrs}>
            <TextBlock {...newProps} />
        </Component>
    );
};
