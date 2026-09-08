import { BlockEditorNode } from '@dotcms/types';

interface DotCMSImageProps {
    src: string;
    alt: string;
    href?: string | null;
    target?: string | null;
    textWrap?: 'left' | 'right';
    textAlign?: string;
}

/**
 * Renders an image component for dotCMS.
 *
 * When the Block Editor assigns a link to the image, it is stored as `href` /
 * `target` on the `dotImage` node and the image is wrapped in an anchor. `href`
 * is `null` when the image has no link, in which case it renders bare. The
 * truthiness check also covers the transient `''` the editor writes while
 * unsetting a link.
 *
 * @param node - The node for the DotCMSImage component.
 * @returns The rendered image component.
 */
export const DotCMSImage = ({ node }: { node: BlockEditorNode }) => {
    const { src, alt, href, target, textWrap, textAlign } = node.attrs as DotCMSImageProps;

    let wrapperStyle: React.CSSProperties = {};

    if (textWrap === 'left') {
        wrapperStyle = { float: 'left', width: '50%', margin: '0 1rem 1rem 0' };
    } else if (textWrap === 'right') {
        wrapperStyle = { float: 'right', width: '50%', margin: '0 0 1rem 1rem' };
    } else if (textAlign) {
        wrapperStyle = { textAlign: textAlign as React.CSSProperties['textAlign'] };
    }

    const image = (
        <img
            alt={alt}
            src={src}
            style={textWrap ? { maxWidth: '100%', height: 'auto' } : undefined}
        />
    );

    return (
        <figure style={wrapperStyle}>
            {href ? (
                <a
                    href={href}
                    target={target ?? undefined}
                    // Guards against reverse tabnabbing when the link opens in a new tab.
                    rel={target === '_blank' ? 'noopener noreferrer' : undefined}>
                    {image}
                </a>
            ) : (
                image
            )}
        </figure>
    );
};
