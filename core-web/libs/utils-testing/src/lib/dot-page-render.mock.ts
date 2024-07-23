import {
    DotPage,
    DotLayout,
    DotPageRenderParameters,
    DotPageMode,
    CONTAINER_SOURCE,
    DotTemplate,
    DotPageContainerStructure
} from '@dotcms/dotcms-models';

import { dotcmsContentTypeBasicMock } from './dot-content-types.mock';
import { mockDotLanguage } from './dot-language.mock';

export const mockDotPage = (): DotPage => {
    return {
        canEdit: true,
        canLock: true,
        canRead: true,
        identifier: '123',
        languageId: 1,
        liveInode: '456',
        lockMessage: '',
        lockedBy: '123',
        lockedByName: 'Some One',
        lockedOn: new Date(1517330917295),
        pageURI: '/an/url/test',
        shortyLive: '',
        shortyWorking: '',
        title: 'A title',
        workingInode: '999',
        contentType: {
            ...dotcmsContentTypeBasicMock,
            defaultType: true,
            fixed: true,
            system: true
        },
        fileAsset: true,
        friendlyName: '',
        host: '',
        inode: '2',
        name: '',
        systemHost: false,
        type: '',
        uri: '',
        versionType: '',
        rendered: '<html><head></header><body><p>Hello World</p></body></html>'
    };
};

export const mockDotLayout = (): DotLayout => {
    return {
        title: '',
        header: false,
        footer: false,
        sidebar: {
            location: 'left',
            width: 'small',
            containers: [
                {
                    identifier: 'fc193c82-8c32-4abe-ba8a-49522328c93e',
                    uuid: 'LEGACY_RELATION_TYPE'
                }
            ]
        },
        body: {
            rows: []
        },
        width: ''
    };
};

export const mockDotContainers = (): DotPageContainerStructure => {
    return {
        '/default/': {
            container: processedContainers[0].container,
            containerStructures: [{ contentTypeVar: 'Banner' }],
            contentlets: {
                '123': [
                    {
                        baseType: '123',
                        content: 'something',
                        contentType: '123',
                        dateCreated: '123',
                        dateModifed: '123',
                        folder: '123',
                        host: '123',
                        identifier: '123',
                        inode: '123',
                        languageId: 123,
                        live: false,
                        locked: false,
                        modDate: '123',
                        modUser: '123',
                        owner: '123',
                        working: false,
                        url: '123',
                        stInode: '123',
                        deleted: false,
                        hostName: '123',
                        archived: false,
                        hasTitleImage: false,
                        image: '123',
                        title: '123',
                        sortOrder: 123,
                        __icon__: '123',
                        modUserName: '123',
                        titleImage: '123'
                    },
                    {
                        baseType: '456',
                        content: 'something',
                        contentType: '456',
                        dateCreated: '456',
                        folder: '456',
                        identifier: '456',
                        inode: '456',
                        languageId: 456,
                        live: false,
                        dateModifed: '456',
                        modDate: '456',
                        host: '456',
                        working: false,
                        title: '456',
                        locked: false,
                        archived: false,
                        owner: '456',
                        url: '456',
                        modUser: '456',
                        __icon__: '456',
                        deleted: false,
                        hasTitleImage: false,
                        titleImage: '456',
                        hostName: '456',
                        sortOrder: 456,
                        image: '456',
                        stInode: '456',
                        modUserName: '456'
                    }
                ],
                '456': [
                    {
                        contentType: '123',
                        content: 'something',
                        dateCreated: '123',
                        baseType: '123',
                        folder: '123',
                        dateModifed: '123',
                        identifier: '123',
                        host: '123',
                        live: false,
                        inode: '123',
                        locked: false,
                        languageId: 123,
                        owner: '123',
                        working: false,
                        modDate: '123',
                        modUser: '123',
                        title: '123',
                        image: '123',
                        archived: false,
                        titleImage: '123',
                        url: '123',
                        __icon__: '123',
                        deleted: false,
                        hasTitleImage: false,
                        hostName: '123',
                        modUserName: '123',
                        stInode: '123',
                        sortOrder: 123
                    }
                ]
            }
        },

        '/banner/': {
            container: processedContainers[1].container,
            containerStructures: [{ contentTypeVar: 'Contact' }],
            contentlets: {
                '123': [
                    {
                        baseType: '123',
                        content: 'something',
                        contentType: '123',
                        dateCreated: '123',
                        dateModifed: '123',
                        folder: '123',
                        host: '123',
                        identifier: '123',
                        inode: '123',
                        languageId: 123,
                        live: false,
                        locked: false,
                        modDate: '123',
                        modUser: '123',
                        owner: '123',
                        working: false,
                        url: '123',
                        stInode: '123',
                        deleted: false,
                        hostName: '123',
                        archived: false,
                        hasTitleImage: false,
                        image: '123',
                        title: '123',
                        sortOrder: 123,
                        __icon__: '123',
                        modUserName: '123',
                        titleImage: '123'
                    },
                    {
                        baseType: '456',
                        content: 'something',
                        contentType: '456',
                        dateCreated: '456',
                        folder: '456',
                        identifier: '456',
                        inode: '456',
                        languageId: 456,
                        live: false,
                        dateModifed: '456',
                        modDate: '456',
                        host: '456',
                        working: false,
                        title: '456',
                        locked: false,
                        archived: false,
                        owner: '456',
                        url: '456',
                        modUser: '456',
                        __icon__: '456',
                        deleted: false,
                        hasTitleImage: false,
                        titleImage: '456',
                        hostName: '456',
                        sortOrder: 456,
                        image: '456',
                        stInode: '456',
                        modUserName: '456'
                    }
                ],
                '456': [
                    {
                        contentType: '123',
                        content: 'something',
                        dateCreated: '123',
                        baseType: '123',
                        folder: '123',
                        dateModifed: '123',
                        identifier: '123',
                        host: '123',
                        live: false,
                        inode: '123',
                        locked: false,
                        languageId: 123,
                        owner: '123',
                        working: false,
                        modDate: '123',
                        modUser: '123',
                        title: '123',
                        image: '123',
                        archived: false,
                        titleImage: '123',
                        url: '123',
                        __icon__: '123',
                        deleted: false,
                        hasTitleImage: false,
                        hostName: '123',
                        modUserName: '123',
                        stInode: '123',
                        sortOrder: 123
                    }
                ]
            }
        }
    };
};

export const processedContainers = [
    {
        container: {
            type: 'containers',
            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
            name: 'Medium Column (md-1)',
            categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93',
            source: CONTAINER_SOURCE.DB,
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        }
    },
    {
        container: {
            type: 'containers',
            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
            name: 'Large Column (lg-1)',
            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
            source: CONTAINER_SOURCE.FILE,
            path: '/container/path',
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        }
    }
];

export const mockDotTemplate = () => {
    return {
        anonymous: false,
        friendlyName: '',
        identifier: '111',
        inode: '123',
        name: '',
        title: 'Template Name',
        type: '',
        versionType: '',
        drawed: true,
        canEdit: true,
        theme: '',
        layout: null as unknown as DotLayout,
        hasLiveVersion: true,
        working: true
    };
};

export const mockDotTemplateLayout: DotTemplate = {
    ...mockDotTemplate(),
    anonymous: false,
    title: 'anonymous_layout_1511798005268'
};

export const mockDotRenderedPage = (): DotPageRenderParameters => {
    return {
        containers: mockDotContainers(),
        layout: mockDotLayout(),
        page: mockDotPage(),
        template: mockDotTemplate(),
        canCreateTemplate: true,
        numberContents: 1,
        viewAs: {
            language: mockDotLanguage,
            mode: DotPageMode.PREVIEW
        }
    };
};
