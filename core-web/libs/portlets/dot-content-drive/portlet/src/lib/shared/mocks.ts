import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotContentDriveItem,
    DotSite,
    StructureTypeView
} from '@dotcms/dotcms-models';
import { createFakeContentType } from '@dotcms/utils-testing';

export const MOCK_ITEMS: DotContentDriveItem[] = [
    { identifier: '123', title: 'Test Content 1', inode: 'inode-123' } as DotContentDriveItem,
    { identifier: '456', title: 'Test Content 2', inode: 'inode-456' } as DotContentDriveItem
];

export const MOCK_SEARCH_RESPONSE = {
    list: MOCK_ITEMS,
    contentTotalCount: 2,
    folderCount: 0,
    contentCount: 2
};

export const MOCK_ROUTE = {
    snapshot: {
        queryParams: {
            path: '/test/path',
            filters: 'contentType:Blog;status:published'
        }
    }
};

/**
 * `DotSite` is deliberately the minimal, normalised site entity — its own doc says "Do NOT use the
 * old `Site` or `SiteEntity` types". These fixtures carried the full legacy host DTO (`categoryId`,
 * `contentTypeId`, `dotAsset`, `folder`, `languageId`, …), which excess-property checking rejects.
 * Trimmed to what the model declares; the specs only read `identifier` and `hostname`.
 */
export const MOCK_SITES: DotSite[] = [
    {
        identifier: 'site-001',
        hostname: 'demo.com',
        aliases: 'demo.com,www.demo.com',
        archived: false
    },
    {
        identifier: 'site-002',
        hostname: 'marketing.example.com',
        aliases: 'marketing.example.com',
        archived: false
    },
    {
        identifier: 'site-003',
        hostname: 'archive.example.com',
        aliases: '',
        archived: true
    },
    {
        identifier: 'SYSTEM_HOST',
        hostname: 'SYSTEM_HOST',
        aliases: '',
        archived: false
    }
];

export const MOCK_CONTENT_TYPES: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'blog',
        baseType: DotCMSBaseTypesContentTypes.CONTENT,
        system: false
    },
    {
        id: '2',
        name: 'News',
        variable: 'news',
        baseType: DotCMSBaseTypesContentTypes.CONTENT,
        system: false
    },
    {
        id: '3',
        name: 'Contact Form',
        variable: 'contactForm',
        baseType: DotCMSBaseTypesContentTypes.FORM,
        system: false
    },
    {
        id: '4',
        name: 'System Content',
        variable: 'systemContent',
        baseType: DotCMSBaseTypesContentTypes.CONTENT,
        system: true
    }
].map(createFakeContentType);

export const SELECTED_CONTENT_TYPES: DotCMSContentType[] = [
    MOCK_CONTENT_TYPES[0],
    MOCK_CONTENT_TYPES[1],
    MOCK_CONTENT_TYPES[2],
    MOCK_CONTENT_TYPES[3]
].filter((ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM); // Select only content types that are not form

export const MOCK_BASE_TYPES: StructureTypeView[] = [
    { name: 'Content', label: 'Content', types: null },
    { name: 'Widget', label: 'Widget', types: null },
    { name: 'FORM', label: 'FORM', types: null },
    { name: 'FileAsset', label: 'FileAsset', types: null }
];
