import { BlockEditorNode } from '@dotcms/types';

interface DotCMSImageProps {
    src: string;
    alt: string;
}

/**
 * Renders an image component for dotCMS.
 *
 * @param node - The node for the DotCMSImage component.
 * @returns The rendered image component.
 */
export const DotCMSImage = ({ node }: { node: BlockEditorNode }) => {
    const { src, alt } = node.attrs as DotCMSImageProps;

    return <img alt={alt} src={src} />;
};
