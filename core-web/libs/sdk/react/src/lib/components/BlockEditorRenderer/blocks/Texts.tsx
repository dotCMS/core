/* eslint-disable @typescript-eslint/no-explicit-any */

// import { useContext } from "react";
// import { PageContext } from "../../../contexts/PageContext";
import { BlockProps } from "../../../models";


type HeadingProps = BlockProps & { level?: string };
// type LinkProps = React.ComponentProps<'a'> & {
//     activeClassName?: string
//   }

export const Bold = ({ children }: BlockProps) => <strong>{children}</strong>;

export const Italic = ({ children }: BlockProps) => <em>{children}</em>;

export const Strike = ({ children }: BlockProps) => <s>{children}</s>;

export const Underline = ({ children }: BlockProps) => <u>{children}</u>;

export const Paragraph = ({ children }: BlockProps) => {
    return <p>{children}</p>;
};

// export const DotLink =  ({
//   as,
//   activeClassName,
//   href,
//   locale,
//   passHref,
//   replace,
//   scroll,
//   shallow,
//   ...rest
// }: any) => {
//   const { isInsideEditor } = useContext(PageContext) as DotCMSPageContext;
//     // If the href is external (i.e. not internal), we don't want to
//   // pass the href to the next/link component. So we'll just pass
//   // the href to a normal anchor tag with a target of _blank and
//   // a rel of noopener noreferrer to prevent the page from being
//   // supplanted.
//   if (href.startsWith('http')) {
//     return <a {...rest} href={href} rel="noopener noreferrer" target="_blank"> </a>
//   }
// }

export const Heading = ({ level, children }: HeadingProps) => {
    const Tag = `h${level}` as keyof JSX.IntrinsicElements;

    return <Tag>{children}</Tag>;
};

const nodeMarks: Record<string, React.FC<any>> = {
    // link: Link,
    bold: Bold,
    underline: Underline,
    italic: Italic,
    strike: Strike
};

export const TextNode = (props: any) => {
    const { marks = [], text } = props;
    const mark = marks[0] || { type: '', attrs: {} };
    const newProps = { ...props, marks: marks.slice(1) };
    const Component = nodeMarks[mark?.type];

    if (!Component) {
        return text;
    }

    return (
        <Component attrs={mark.attrs}>
            <TextNode {...newProps} />
        </Component>
    );
};

