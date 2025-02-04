import { DotCmsClient } from '@dotcms/client';

import { ContentNode, DotCMSImageProps } from '../../../models/content-node.interface';

/**
 * Renders an image component for dotCMS.
 *
 * @param props - The props for the DotCMSImage component.
 * @returns The rendered image component.
 */
export const DotCMSImage = (props: ContentNode) => {
    const { data, src, alt } = props.attrs as DotCMSImageProps;
    const client = DotCmsClient.instance;

    const srcUrl = data?.identifier ? `${client.dotcmsUrl}${src}` : src;

    return <img alt={alt} src={srcUrl} />;
};
