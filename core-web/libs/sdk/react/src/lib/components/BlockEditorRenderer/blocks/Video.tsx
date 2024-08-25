import { DotCmsClient } from '@dotcms/client';

import { ContentNode } from '../../../models/blocks.interface';

type DotCMSVideoProps = ContentNode['attrs'] & {
    data?: Record<string, unknown>;
};

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
            <source src={srcUrl} type={mimeType} />
            Tu navegador no soporta el elemento <code>video</code>.
        </video>
    );
};
