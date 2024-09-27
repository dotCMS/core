import { DotCMSContentlet, DotFileMetadata } from '@dotcms/dotcms-models';

export const getFileMetadata = (contentlet: DotCMSContentlet): DotFileMetadata => {
    const { metaData } = contentlet;

    const metadata = metaData || contentlet[`assetMetaData`];

    return metadata || {};
};

export const getFileVersion = (contentlet: DotCMSContentlet) => {
    return contentlet['assetVersion'] || null;
};
