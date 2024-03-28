import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { getImageAssetUrl } from '@dotcms/utils';

export const formatDotImageNode = (asset: DotCMSContentlet) => {
    return `<img src="${getImageAssetUrl(asset)}"
    alt="${asset.title}"
    data-field-name="${asset.titleImage}"
    data-inode="${asset.inode}"
    data-identifier="${asset.identifier}"
    data-saveas="${asset.title}" />`;
};
