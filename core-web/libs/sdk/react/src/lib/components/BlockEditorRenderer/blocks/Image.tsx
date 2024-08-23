/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCmsClient } from '@dotcms/client';

// Maybe we can reuse the "data" type from another file.
export type DotCMSImageProps = Record<string, string | any>;

export const DotCMSImage = ({ data, alt, src }: DotCMSImageProps) => {
    const client = DotCmsClient.instance;

    const srcUrl = data.identifier ? `${client.dotcmsUrl}${src}` : src;

    return <img alt={alt} src={srcUrl} />;
};
