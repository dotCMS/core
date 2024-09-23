import { DotCMSContentlet, DotFileMetadata } from '@dotcms/dotcms-models';

export const getFileMetadata = (
    contentlet: DotCMSContentlet,
    fieldVariable: string
): DotFileMetadata => {
    const { metaData } = contentlet;

    const metadata = metaData || contentlet[`${fieldVariable}MetaData`];

    return metadata || {};
};
