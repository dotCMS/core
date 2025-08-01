import { DotContentDriveItem } from '@dotcms/dotcms-models';

export const mockItems: DotContentDriveItem[] = [
    { identifier: '123', title: 'Test Content 1' } as DotContentDriveItem,
    { identifier: '456', title: 'Test Content 2' } as DotContentDriveItem
];

export const mockSearchResponse = {
    jsonObjectView: {
        contentlets: mockItems
    },
    resultsSize: 2
};

export const mockRoute = {
    snapshot: {
        queryParams: {
            path: '/test/path',
            filters: 'contentType:Blog;status:published'
        }
    }
};
