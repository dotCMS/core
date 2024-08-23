import { DotCmsClient } from '@dotcms/client';

import { DotAssetProps } from '../../../models/blocks.interface';

export const DotCMSImage = (props: DotAssetProps) => {
    const { data, src, alt } = props;
    const client = DotCmsClient.instance;

    const srcUrl = data.identifier ? `${client.dotcmsUrl}${src}` : src;

    return <img alt={alt} src={srcUrl} />;
};
