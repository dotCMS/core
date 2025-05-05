import { BlockEditorNode } from '@dotcms/types';

interface DotCMSImageProps {
    data: {
        identifier: string;
    };
    src: string;
    alt: string;
}

/**
 * Renders an image component for dotCMS.
 *
 * @param node - The node for the DotCMSImage component.
 * @returns The rendered image component.
 */
export const DotCMSImage = (node: BlockEditorNode) => {
    const { data, src, alt } = node.attrs as DotCMSImageProps;
    // Fix this path
    const srcUrl = data?.identifier ? `${''}${src}` : src;

    return <img alt={alt} src={srcUrl} />;
};
