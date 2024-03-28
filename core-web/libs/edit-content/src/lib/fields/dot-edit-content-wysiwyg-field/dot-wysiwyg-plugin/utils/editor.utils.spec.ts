import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { formatDotImageNode } from './editor.utils';

describe('formatDotImageNode', () => {
    it('should return formatted image node', () => {
        const asset: DotCMSContentlet = {
            ...EMPTY_CONTENTLET,
            baseType: 'DOTASSET',
            assetVersion: 'version',
            asset: 'asset',
            title: 'title',
            titleImage: 'titleImage',
            inode: 'inode',
            identifier: 'identifier'
        };

        const result = formatDotImageNode(asset);

        expect(result).toBe(
            `<img src="${asset.assetVersion || asset.asset}"
    alt="${asset.title}"
    data-field-name="${asset.titleImage}"
    data-inode="${asset.inode}"
    data-identifier="${asset.identifier}"
    data-saveas="${asset.title}" />`
        );
    });
});
