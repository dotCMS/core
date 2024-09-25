import { DotCMSContentlet, DotFileMetadata } from '@dotcms/dotcms-models';

export const getFileMetadata = (contentlet: DotCMSContentlet): DotFileMetadata => {
    console.log('contentlet', contentlet);

    const { metaData } = contentlet;

    const metadata = metaData || contentlet[`assetMetaData`];

    return metadata || {};
};
