import { DotCmsClient } from '@dotcms/client';

import { ContentNode, DotCMSVideoProps } from '../../../models/content-node.interface';

/**
 * Renders a video component for displaying videos.
 *
 * @param props - The properties for the video component.
 * @returns The rendered video component.
 */
export const DotCMSVideo = (props: ContentNode) => {
    const { data, src, mimeType, width, height } = props.attrs as DotCMSVideoProps;
    const client = DotCmsClient.instance;

    const srcUrl = data?.identifier ? `${client.dotcmsUrl}${src}` : src;

    const poster = data?.thumbnail ? `${client.dotcmsUrl}${data?.thumbnail}` : 'poster-image.jpg';

    return (
        <video controls preload="metadata" poster={poster} width={width} height={height}>
            <track default kind="captions" srcLang="en" />
            <source src={srcUrl} type={mimeType} />
            Your browser does not support the <code>video</code> element.
        </video>
    );
};
