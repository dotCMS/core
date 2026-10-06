import { BundleAssetView } from '@dotcms/dotcms-models';

import { groupContentletAssetsByType } from './asset-groups.util';

const asset = (over: Partial<BundleAssetView> = {}): BundleAssetView => ({
    asset: over.asset ?? 'a-1',
    title: 'Title',
    type: 'contentlet',
    inode: 'inode-1',
    content_type_name: 'Blog',
    ...over
});

describe('groupContentletAssetsByType', () => {
    it('returns an empty map when the input is empty', () => {
        expect(groupContentletAssetsByType([]).size).toBe(0);
    });

    it('skips assets whose type is not contentlet', () => {
        const result = groupContentletAssetsByType([
            asset({ asset: 'c1', type: 'contentlet' }),
            asset({ asset: 't1', type: 'template' }),
            asset({ asset: 'l1', type: 'language' })
        ]);
        expect(result.size).toBe(1);
        expect(result.get('Blog')?.map((a) => a.asset)).toEqual(['c1']);
    });

    it('skips contentlet assets without an inode', () => {
        const result = groupContentletAssetsByType([
            asset({ asset: 'c1', inode: 'i1' }),
            asset({ asset: 'c2', inode: undefined })
        ]);
        expect(result.get('Blog')?.map((a) => a.asset)).toEqual(['c1']);
    });

    it('groups contentlets sharing a content type together, preserving order', () => {
        const result = groupContentletAssetsByType([
            asset({ asset: 'c1', content_type_name: 'Blog' }),
            asset({ asset: 'c2', content_type_name: 'News' }),
            asset({ asset: 'c3', content_type_name: 'Blog' }),
            asset({ asset: 'c4', content_type_name: 'News' })
        ]);
        expect(result.get('Blog')?.map((a) => a.asset)).toEqual(['c1', 'c3']);
        expect(result.get('News')?.map((a) => a.asset)).toEqual(['c2', 'c4']);
    });

    it('skips contentlet assets with missing content_type_name (edit-URL cache collides otherwise)', () => {
        const result = groupContentletAssetsByType([
            asset({ asset: 'c1', content_type_name: undefined }),
            asset({ asset: 'c2', content_type_name: undefined })
        ]);
        expect(result.size).toBe(0);
    });
});
