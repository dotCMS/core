import { BlockProps, ContentNode, Mark } from '../../../models/blocks.interface';

type HeadingProps = BlockProps & { level?: string };
type LinkProps = BlockProps & { attrs?: Mark['attrs'] };
type ParagraphProps = BlockProps & { attrs?: Mark['attrs'] };

export const Bold = ({ children }: BlockProps) => <strong>{children}</strong>;

export const Italic = ({ children }: BlockProps) => <em>{children}</em>;

export const Strike = ({ children }: BlockProps) => <s>{children}</s>;

export const Underline = ({ children }: BlockProps) => <u>{children}</u>;

export const Paragraph = ({ children, attrs }: ParagraphProps) => {
    return <p style={attrs}>{children}</p>;
};

export const Link = ({ children, attrs }: LinkProps) => {
    return <a {...attrs}>{children}</a>;
};

export const Heading = ({ level, children }: HeadingProps) => {
    const Tag = `h${level}` as keyof JSX.IntrinsicElements;

    return <Tag>{children}</Tag>;
};

export const Superscript = ({ children }: BlockProps) => <sup>{children}</sup>;

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
