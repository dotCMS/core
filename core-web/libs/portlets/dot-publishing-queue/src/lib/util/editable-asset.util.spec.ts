import { BundleAssetView } from '@dotcms/dotcms-models';

import { isEditableContentlet } from './editable-asset.util';

const asset = (over: Partial<BundleAssetView> = {}): BundleAssetView => ({
    asset: over.asset ?? 'a-1',
    title: 'Title',
    type: 'contentlet',
    inode: 'inode-1',
    content_type_name: 'Blog',
    ...over
});

describe('isEditableContentlet', () => {
    it('accepts a contentlet naming its inode and content type', () => {
        expect(isEditableContentlet(asset())).toBe(true);
    });

    it.each(['template', 'language', 'container'])('rejects a %s', (type) => {
        expect(isEditableContentlet(asset({ type }))).toBe(false);
    });

    it('rejects a contentlet without an inode', () => {
        expect(isEditableContentlet(asset({ inode: undefined }))).toBe(false);
    });

    // The opener needs the type to pick the editor without another request.
    it('rejects a contentlet without a content type', () => {
        expect(isEditableContentlet(asset({ content_type_name: undefined }))).toBe(false);
    });
});
