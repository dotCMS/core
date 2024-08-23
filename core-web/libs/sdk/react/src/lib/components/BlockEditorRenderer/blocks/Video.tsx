import { DotCmsClient } from '@dotcms/client';

import { DotAssetProps } from '../../../models/blocks.interface';

export const DotCMSVideo = ({ data, src, width, height, mimeType }: DotAssetProps) => {

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
