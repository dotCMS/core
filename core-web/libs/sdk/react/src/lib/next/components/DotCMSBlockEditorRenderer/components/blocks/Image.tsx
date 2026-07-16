import { BlockEditorNode } from '@dotcms/types';

interface DotCMSImageProps {
    src: string;
    alt: string;
    textWrap?: 'left' | 'right';
    textAlign?: string;
}

/**
 * Renders an image component for dotCMS.
 *
 * @param node - The node for the DotCMSImage component.
 * @returns The rendered image component.
 */
export const DotCMSImage = ({ node }: { node: BlockEditorNode }) => {
    const { src, alt, textWrap, textAlign } = node.attrs as DotCMSImageProps;

    let wrapperStyle: React.CSSProperties = {};

    if (textWrap === 'left') {
        wrapperStyle = { float: 'left', width: '50%', margin: '0 1rem 1rem 0' };
    } else if (textWrap === 'right') {
        wrapperStyle = { float: 'right', width: '50%', margin: '0 0 1rem 1rem' };
    } else if (textAlign) {
        wrapperStyle = { textAlign: textAlign as React.CSSProperties['textAlign'] };
    }

    return (
        <figure style={wrapperStyle}>
            <img
                alt={alt}
                src={src}
                style={textWrap ? { maxWidth: '100%', height: 'auto' } : undefined}
            />
        </figure>
    );
};
