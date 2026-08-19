import { ALL_FOLDER } from '@dotcms/ui';

describe('ALL_FOLDER constant', () => {
    it('should have correct structure', () => {
        expect(ALL_FOLDER).toEqual({
            key: 'ALL_FOLDER',
            label: 'content-drive.all-folder.label',
            loading: false,
            data: {
                type: 'folder',
                path: '',
                hostname: '',
                id: '',
                inode: ''
            },
            icon: 'pi pi-folder',
            leaf: false,
            expanded: true
        });
    });

    it('should be a folder type', () => {
        expect(ALL_FOLDER.data.type).toBe('folder');
    });

    it('should be expanded by default', () => {
        expect(ALL_FOLDER.expanded).toBe(true);
    });

    it('should not be a leaf node', () => {
        expect(ALL_FOLDER.leaf).toBe(false);
    });

    it('should use a native PrimeNG folder icon', () => {
        expect(ALL_FOLDER.icon).toBe('pi pi-folder');
    });
});
