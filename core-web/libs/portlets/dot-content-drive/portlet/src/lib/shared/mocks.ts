import {
    DotCMSContentType,
    DotContentDriveItem,
    SiteEntity,
    StructureTypeView
} from '@dotcms/dotcms-models';

import { BASE_TYPES } from './models';

export const MOCK_ITEMS: DotContentDriveItem[] = [
    { identifier: '123', title: 'Test Content 1' } as DotContentDriveItem,
    { identifier: '456', title: 'Test Content 2' } as DotContentDriveItem
];

export const MOCK_SEARCH_RESPONSE = {
    jsonObjectView: {
        contentlets: MOCK_ITEMS
    },
    resultsSize: 2
};

export const MOCK_ROUTE = {
    snapshot: {
        queryParams: {
            path: '/test/path',
            filters: 'contentType:Blog;status:published'
        }
    }
};

export const MOCK_SITES: SiteEntity[] = [
    {
        aliases: 'demo.com,www.demo.com',
        archived: false,
        categoryId: 'cat-demo',
        contentTypeId: 'ct-host',
        default: true,
        dotAsset: false,
        fileAsset: false,
        folder: '/',
        form: false,
        host: 'demo.com',
        hostThumbnail: null,
        hostname: 'demo.com',
        htmlpage: true,
        identifier: 'site-001',
        indexPolicyDependencies: '',
        inode: 'inode-001',
        keyValue: false,
        languageId: 1,
        languageVariable: false,
        live: true,
        locked: false,
        lowIndexPriority: false,
        modDate: 1710000000000,
        modUser: 'admin',
        name: 'Demo Site',
        new: false,
        owner: 'admin',
        parent: false,
        permissionId: 'perm-001',
        permissionType: 'INDIVIDUAL',
        persona: false,
        sortOrder: 0,
        structureInode: '',
        systemHost: false,
        tagStorage: 'SCHEMA',
        title: 'Demo Site',
        titleImage: null,
        type: 'host',
        vanityUrl: false,
        variantId: 'variant-001',
        versionId: 'version-001',
        working: true
    },
    {
        aliases: 'marketing.example.com',
        archived: false,
        categoryId: 'cat-mkt',
        contentTypeId: 'ct-host',
        default: false,
        dotAsset: false,
        fileAsset: false,
        folder: '/',
        form: false,
        host: 'marketing.example.com',
        hostThumbnail: null,
        hostname: 'marketing.example.com',
        htmlpage: true,
        identifier: 'site-002',
        indexPolicyDependencies: '',
        inode: 'inode-002',
        keyValue: false,
        languageId: 1,
        languageVariable: false,
        live: true,
        locked: false,
        lowIndexPriority: false,
        modDate: 1711000000000,
        modUser: 'editor',
        name: 'Marketing Site',
        new: false,
        owner: 'marketing',
        parent: false,
        permissionId: 'perm-002',
        permissionType: 'INDIVIDUAL',
        persona: false,
        sortOrder: 1,
        structureInode: '',
        systemHost: false,
        tagStorage: 'SCHEMA',
        title: 'Marketing Site',
        titleImage: null,
        type: 'host',
        vanityUrl: true,
        variantId: 'variant-002',
        versionId: 'version-002',
        working: true
    },
    {
        aliases: '',
        archived: true,
        categoryId: 'cat-archive',
        contentTypeId: 'ct-host',
        default: false,
        dotAsset: false,
        fileAsset: false,
        folder: '/',
        form: false,
        host: 'archive.example.com',
        hostThumbnail: null,
        hostname: 'archive.example.com',
        htmlpage: false,
        identifier: 'site-003',
        indexPolicyDependencies: '',
        inode: 'inode-003',
        keyValue: false,
        languageId: 1,
        languageVariable: false,
        live: false,
        locked: false,
        lowIndexPriority: true,
        modDate: 1700000000000,
        modUser: 'archiver',
        name: 'Archived Site',
        new: false,
        owner: 'ops',
        parent: false,
        permissionId: 'perm-003',
        permissionType: 'INDIVIDUAL',
        persona: false,
        sortOrder: 2,
        structureInode: '',
        systemHost: false,
        tagStorage: 'SCHEMA',
        title: 'Archived Site',
        titleImage: null,
        type: 'host',
        vanityUrl: false,
        variantId: 'variant-003',
        versionId: 'version-003',
        working: false
    },
    {
        aliases: '',
        archived: false,
        categoryId: '',
        contentTypeId: '',
        default: false,
        dotAsset: false,
        fileAsset: false,
        folder: '/',
        form: false,
        host: 'SYSTEM_HOST',
        hostThumbnail: null,
        hostname: 'SYSTEM_HOST',
        htmlpage: false,
        identifier: 'SYSTEM_HOST',
        indexPolicyDependencies: '',
        inode: 'SYSTEM_HOST',
        keyValue: false,
        languageId: 1,
        languageVariable: false,
        live: true,
        locked: false,
        lowIndexPriority: false,
        modDate: 0,
        modUser: '',
        name: 'System Host',
        new: false,
        owner: '',
        parent: false,
        permissionId: '',
        permissionType: 'INDIVIDUAL',
        persona: false,
        sortOrder: 99,
        structureInode: '',
        systemHost: true,
        tagStorage: 'SCHEMA',
        title: 'System Host',
        titleImage: null,
        type: 'HOST',
        vanityUrl: false,
        variantId: '',
        versionId: '',
        working: true
    }
];

export const MOCK_CONTENT_TYPES: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'blog',
        baseType: BASE_TYPES.content,
        system: false
    } as DotCMSContentType,
    {
        id: '2',
        name: 'News',
        variable: 'news',
        baseType: BASE_TYPES.content,
        system: false
    } as DotCMSContentType,
    {
        id: '3',
        name: 'Contact Form',
        variable: 'contactForm',
        baseType: BASE_TYPES.form,
        system: false
    } as DotCMSContentType,
    {
        id: '4',
        name: 'System Content',
        variable: 'systemContent',
        baseType: BASE_TYPES.content,
        system: true
    } as DotCMSContentType
];

export const MOCK_BASE_TYPES: StructureTypeView[] = [
    { name: 'Content', label: 'Content', types: null },
    { name: 'Widget', label: 'Widget', types: null },
    { name: 'FORM', label: 'FORM', types: null },
    { name: 'FileAsset', label: 'FileAsset', types: null }
];
