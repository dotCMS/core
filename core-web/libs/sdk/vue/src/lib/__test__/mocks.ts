import type {
    DotCMSBasicContentlet,
    DotCMSColumnContainer,
    DotCMSPageAsset,
    DotPageAssetLayoutColumn
} from '@dotcms/types';

export const MOCK_COLUMN: DotPageAssetLayoutColumn = {
    left: 0,
    width: 6,
    leftOffset: 2,
    preview: false,
    widthPercent: 50,
    styleClass: 'custom-column-class',
    containers: [{ identifier: 'test-container-id', uuid: 'test-uuid', historyUUIDs: [] }]
} as unknown as DotPageAssetLayoutColumn;

export const MOCK_CONTAINER: DotCMSColumnContainer = {
    identifier: 'test-container-id',
    uuid: 'test-uuid',
    historyUUIDs: []
} as unknown as DotCMSColumnContainer;

export const MOCK_CONTENTLET: DotCMSBasicContentlet = {
    identifier: 'contentlet-1',
    inode: 'inode-1',
    title: 'Test Banner',
    contentType: 'Banner',
    baseType: 'CONTENT',
    languageId: 1
} as unknown as DotCMSBasicContentlet;

export const MOCK_PAGE_ASSET = {
    layout: {
        header: true,
        footer: true,
        body: {
            rows: [
                {
                    styleClass: '',
                    columns: [MOCK_COLUMN]
                }
            ]
        }
    },
    page: { pageURI: '/test', title: 'Test Page' },
    containers: {
        'test-container-id': {
            container: { identifier: 'test-container-id', maxContentlets: 10 },
            containerStructures: [{ contentTypeVar: 'Banner' }],
            contentlets: { 'uuid-test-uuid': [MOCK_CONTENTLET] }
        }
    }
} as unknown as DotCMSPageAsset;

export const PAGE_ASSET_NO_BODY = {
    layout: { header: true, footer: true }
} as unknown as DotCMSPageAsset;
