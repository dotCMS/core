import { of } from 'rxjs';

import { InjectionToken } from '@angular/core';

import { mockSites } from '@dotcms/dotcms-js';
import {
    CONTAINER_SOURCE,
    DEFAULT_VARIANT_ID,
    DotPageContainerStructure,
    DotPersona
} from '@dotcms/dotcms-models';
import {
    mockDotLayout,
    mockDotTemplate,
    mockDotContainers,
    dotcmsContentletMock
} from '@dotcms/utils-testing';

import { EDITOR_MODE, EDITOR_STATE } from './enums';
import { ActionPayload } from './models';

import { DotPageApiResponse } from '../services/dot-page-api.service';

export const LAYOUT_URL = '/c/portal/layout';

export const CONTENTLET_SELECTOR_URL = `/html/ng-contentlet-selector.jsp`;

export const HOST = 'http://localhost:3000';

export const WINDOW = new InjectionToken<Window>('WindowToken');

export const EDIT_CONTENT_CALLBACK_FUNCTION = 'saveAssignCallBackAngular';

export const VIEW_CONTENT_CALLBACK_FUNCTION = 'angularWorkflowEventCallback';

export const IFRAME_SCROLL_ZONE = 100;

export const DEFAULT_PERSONA: DotPersona = {
    hostFolder: 'SYSTEM_HOST',
    inode: '',
    host: 'SYSTEM_HOST',
    locked: false,
    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
    contentType: 'persona',
    identifier: 'modes.persona.no.persona',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: false,
    owner: 'SYSTEM_USER',
    url: 'demo.dotcms.com',
    sortOrder: 0,
    name: 'Default Visitor',
    hostName: 'System Host',
    modDate: '0',
    title: 'Default Visitor',
    personalized: false,
    baseType: 'PERSONA',
    archived: false,
    working: false,
    live: false,
    keyTag: 'dot:persona',
    languageId: 1,
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    modUserName: 'system user system user',
    hasLiveVersion: false,
    modUser: 'system'
};

export const PAYLOAD_MOCK: ActionPayload = {
    container: {
        acceptTypes: 'Banner',
        contentletsId: ['19c5ecc0c59b17b5780acd624ad52444', '2e5d54e6-7ea3-4d72-8577-b8731b206ca0'],
        identifier: '//demo.dotcms.com/application/containers/banner/',
        maxContentlets: 25,
        uuid: '1',
        variantId: '1'
    },
    contentlet: {
        identifier: '19c5ecc0c59b17b5780acd624ad52444',
        title: 'Zelda Cafe',
        inode: 'ff10d5db-b06e-4298-870b-fbe2f5001ac2',
        onNumberOfPages: 1,
        contentType: 'Banner'
    },
    language_id: '1',
    pageContainers: [
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '10',
            contentletsId: ['c151d3f0-9572-4bcc-8c8f-00c9da9758e0']
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '1',
            contentletsId: ['df591adf-10fe-461a-a12e-f847df0fd2fb']
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '2',
            contentletsId: [
                '4694d40b-d9be-4e09-b031-64ee3e7c9642',
                '6ac5921e-e062-49a6-9808-f41aff9343c5'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '3',
            contentletsId: [
                '574f0aec-185a-4160-9c17-6d037b298318',
                '50351143-3ba6-4c54-9e9b-0d8a90d2f9b0'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '4',
            contentletsId: [
                '8ccfa397-4369-44bb-b450-33151387eb02',
                '6a8102b5-fdb0-4ad5-9a5d-e982bcdb54c8'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '5',
            contentletsId: [
                '50757fb4-75df-4e2c-8335-35d36bdb944b',
                '0e9340f8-08d2-46e3-ae25-be0137c575d0'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '6',
            contentletsId: [
                'f40c6030-3532-4e75-9ca8-0d92261264e3',
                'd1f449ec-ad6a-4b59-ab38-98ba2e9c3231',
                'b0df5dbd-0e3c-4df8-b478-55b4d9cee344'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '7',
            contentletsId: [
                '39aa1441-2933-4c81-b3f4-cc154195595b',
                'acfcb298-af27-40bc-835b-faf634b0f888',
                '46e52dc2-e72a-4641-8925-026abf2adccd'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '8',
            contentletsId: [
                'e9fdab13-72f2-486c-b645-0e2315d5c33b',
                '5985eec0-6bc7-4d87-be13-d4dc83516da2',
                'df26d95c-4f1f-4503-ac81-8176bb1c417d'
            ]
        },
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '9',
            contentletsId: ['cbe5573b-a201-477b-aea1-5ff3e75a1072']
        },
        {
            identifier: '//demo.dotcms.com/application/containers/banner/',
            uuid: '1',
            contentletsId: [
                '19c5ecc0c59b17b5780acd624ad52444',
                '2e5d54e6-7ea3-4d72-8577-b8731b206ca0'
            ]
        }
    ],
    pageId: 'a9f30020-54ef-494e-92ed-645e757171c2',
    position: 'before'
};

export const MOCK_RESPONSE_HEADLESS: DotPageApiResponse = {
    page: {
        pageURI: 'test-url',
        title: 'Test Page',
        identifier: '123',
        inode: '123-i',
        canEdit: true,
        canRead: true,
        contentType: 'htmlpageasset',
        canLock: true,
        locked: false,
        lockedBy: '',
        lockedByName: '',
        live: true
    },
    viewAs: {
        language: {
            id: 1,
            language: 'English',
            countryCode: 'US',
            languageCode: '1',
            country: 'United States'
        },
        variantId: DEFAULT_VARIANT_ID,
        persona: {
            ...DEFAULT_PERSONA
        }
    },
    site: mockSites[0],
    layout: mockDotLayout(),
    template: mockDotTemplate(),
    containers: mockDotContainers()
};

export const dotPageContainerStructureMock: DotPageContainerStructure = {
    '123': {
        container: {
            archived: false,
            categoryId: '123',
            deleted: false,
            friendlyName: '123',
            identifier: '123',
            live: false,
            locked: false,
            maxContentlets: 123,
            name: '123',
            path: '123',
            pathName: '123',
            postLoop: '123',
            preLoop: '123',
            source: CONTAINER_SOURCE.DB,
            title: '123',
            type: '123',
            working: false
        },
        containerStructures: [
            {
                contentTypeVar: '123'
            }
        ],
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

export const PAGE_INODE_MOCK = '1234';

export const QUERY_PARAMS_MOCK = { language_id: 1, url: 'page-one' };

export const TREE_NODE_MOCK = {
    containerId: '123',
    contentId: '123',
    pageId: '123',
    relationType: 'test',
    treeOrder: '1',
    variantId: 'test',
    personalization: 'dot:default'
};

export const newContentlet = {
    ...dotcmsContentletMock,
    inode: '123',
    title: 'test'
};

export const EDIT_ACTION_PAYLOAD_MOCK: ActionPayload = {
    language_id: '1',
    pageContainers: [
        {
            identifier: 'test',
            uuid: 'test',
            contentletsId: []
        }
    ],
    contentlet: {
        identifier: 'contentlet-identifier-123',
        inode: 'contentlet-inode-123',
        title: 'Hello World',
        contentType: 'test',
        onNumberOfPages: 1
    },
    container: {
        identifier: 'test',
        acceptTypes: 'test',
        uuid: 'test',
        maxContentlets: 1,
        contentletsId: ['123'],
        variantId: '123'
    },
    pageId: 'test',
    position: 'before'
};

export const URL_CONTENT_MAP_MOCK = {
    contentType: 'Blog',
    identifier: '123',
    inode: '1234',
    title: 'hello world'
};

export const SHOW_CONTENTLET_TOOLS_PATCH_MOCK = {
    editorState: EDITOR_STATE.IDLE,
    editorData: {
        mode: EDITOR_MODE.EDIT,
        canEditVariant: true,
        device: null,
        page: {
            lockedByUser: '',
            canLock: true,
            isLocked: false
        }
    },
    contentletArea: {
        x: 0,
        y: 0,
        width: 100,
        height: 100,
        payload: {
            language_id: '',
            pageContainers: [],
            pageId: '',
            container: {
                acceptTypes: '',
                identifier: '',
                maxContentlets: 0,
                variantId: '',
                uuid: ''
            },
            contentlet: {
                identifier: '123',
                inode: '',
                title: '',
                contentType: ''
            }
        }
    }
};

export const PAGE_RESPONSE_BY_LANGUAGE_ID = {
    1: of({
        page: {
            title: 'hello world',
            identifier: '123',
            inode: '123',
            canEdit: true,
            canRead: true,
            pageURI: 'index',
            liveInode: '1234',
            stInode: '12345',
            live: true
        },
        viewAs: {
            language: {
                id: 1,
                language: 'English',
                countryCode: 'US',
                languageCode: 'EN',
                country: 'United States'
            },
            persona: DEFAULT_PERSONA
        },
        site: mockSites[0],
        template: {
            drawed: true
        }
    }),

    2: of({
        page: {
            title: 'hello world',
            identifier: '123',
            inode: '123',
            canEdit: true,
            canRead: true,
            pageURI: 'index',
            liveInode: '1234',
            stInode: '12345',
            live: true
        },
        viewAs: {
            language: {
                id: 2,
                languageCode: 'IT',
                countryCode: '',
                language: 'Italian',
                country: 'Italy'
            },
            persona: DEFAULT_PERSONA
        },
        site: mockSites[0],
        template: {
            drawed: true
        }
    })
};
