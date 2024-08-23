/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCmsClient } from '@dotcms/client';

// Maybe we can reuse the "data" type from another file.
export type DotCMSVideoProps = Record<string, string | any>;

export const DotCMSVideo = ({ data, src, width, height, mimeType }: DotCMSVideoProps) => {
    const client = DotCmsClient.instance;

    const { thumbnail } = data;
    const srcUrl = data.identifier ? `${client.dotcmsUrl}${src}` : src;

    const poster = thumbnail ? `${client.dotcmsUrl}${thumbnail}` : 'poster-image.jpg';

    return (
        <video controls preload="metadata" poster={poster} width={width} height={height}>
            <source src={srcUrl} type={mimeType} />
            Tu navegador no soporta el elemento <code>video</code>.
        </video>
    );
};
