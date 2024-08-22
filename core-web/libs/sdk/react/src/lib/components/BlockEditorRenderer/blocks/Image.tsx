import { DotCmsClient } from '@dotcms/client'

// TODO: Add type to this.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type DotCMSImageProps = Record<string, string | any>;

export const DotCMSImage = ({ data, alt, src }: DotCMSImageProps) => {
    const client = DotCmsClient.instance;

    const srcUrl = data.identifier ? `${client.dotcmsUrl}${src}` : src

    return <img  alt={alt} src={srcUrl} />;
};
