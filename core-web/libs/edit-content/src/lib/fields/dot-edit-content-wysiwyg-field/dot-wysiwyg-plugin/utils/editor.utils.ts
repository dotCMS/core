import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const formatDotImageNode = (asset: DotCMSContentlet) => {
    return `<img src="${asset.assetVersion || asset.asset}"
    alt="${asset.title}"
    data-field-name="${asset.titleImage}"
    data-inode="${asset.inode}"
    data-identifier="${asset.identifier}"
    data-saveas="${asset.title}" />`;
};
