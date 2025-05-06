import {
    DotCMSColumnContainer,
    DotCMSPageAsset,
    DotPageAssetLayoutColumn,
    DotCMSBasicContentlet
} from '@dotcms/types';

export const MOCK_COLUMN: DotPageAssetLayoutColumn = {
    left: 0,
    width: 6,
    leftOffset: 2,
    preview: false,
    widthPercent: 50,
    styleClass: 'custom-column-class',
    containers: [
        {
            identifier: 'container-1',
            uuid: 'uuid-1',
            historyUUIDs: []
        },
        {
            identifier: 'container-2',
            uuid: 'uuid-2',
            historyUUIDs: []
        }
    ]
};

export const MOCK_CONTAINER: DotCMSColumnContainer = {
    identifier: 'test-container-id',
    uuid: 'test-uuid',
    historyUUIDs: []
};

export const MOCK_PAGE_ASSET = {
    layout: {
        body: {
            rows: [
                { id: 1, content: 'Row 1 Content' },
                { id: 2, content: 'Row 2 Content' }
            ]
        }
    },
    containers: {
        'test-container-id': {
            identifier: 'test-container-id',
            title: 'Test Container'
        }
    },
    contentlets: {
        'test-container-id': [{ identifier: 'contentlet-1' }, { identifier: 'contentlet-2' }]
    }
} as unknown as DotCMSPageAsset;

export const EMPTY_PAGE_ASSET = {
    ...MOCK_PAGE_ASSET,
    contentlets: { 'test-container-id': [] }
} as unknown as DotCMSPageAsset;

export const MOCK_CONTAINER_DATA = {
    uuid: 'test-uuid',
    identifier: 'test-container-id',
    acceptTypes: 'test-accept-types',
    maxContentlets: 10
};

export const MOCK_CONTENTLET: DotCMSBasicContentlet = {
    archived: false,
    baseType: '',
    contentType: '',
    folder: '',
    hasTitleImage: false,
    host: '',
    hostName: '',
    identifier: '',
    inode: '',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '',
    modUser: '',
    modUserName: '',
    owner: '',
    sortOrder: 1,
    stInode: '',
    title: 'This is my editable title',
    titleImage: '',
    url: '',
    working: false
};
