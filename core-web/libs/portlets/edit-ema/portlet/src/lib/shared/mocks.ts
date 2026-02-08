import { of } from 'rxjs';

import { CurrentUser } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, CONTAINER_SOURCE, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotCMSLayout,
    DotCMSPageAsset,
    DotCMSSite,
    DotCMSViewAsPersona,
    DotCMSPageAssetContainers,
    DotCMSPage,
    DotCMSTemplate,
    DotCMSContainerStructure,
    DotCMSContainer,
    DotCMSVanityUrl,
    DotCMSURLContentMap
} from '@dotcms/types';
import {
    mockSites,
    mockDotLayout,
    mockDotTemplate,
    mockDotContainers,
    dotcmsContentletMock
} from '@dotcms/utils-testing';

import { DEFAULT_PERSONA, PERSONA_KEY } from './consts';
import { ActionPayload, ClientData } from './models';

import {
    EmaDragItem,
    ContentletArea,
    Container
} from '../edit-ema-editor/components/ema-page-dropzone/types';

export const HEADLESS_BASE_QUERY_PARAMS = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: DEFAULT_PERSONA.keyTag,
    variantName: DEFAULT_VARIANT_ID,
    clientHost: 'http://localhost:3000'
};

export const VTL_BASE_QUERY_PARAMS = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: DEFAULT_PERSONA.keyTag,
    variantName: DEFAULT_VARIANT_ID
};

export const PAYLOAD_MOCK: ActionPayload = {
    container: {
        acceptTypes: 'Banner',
        contentletsId: ['19c5ecc0c59b17b5780acd624ad52444', '2e5d54e6-7ea3-4d72-8577-b8731b206ca0'],
        identifier: '//demo.dotcms.com/application/containers/banner/',
        maxContentlets: 25,
        uuid: '1'
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

export const MOCK_RESPONSE_HEADLESS: DotCMSPageAsset = {
    page: {
        pageURI: 'test-url',
        title: 'Test Page',
        identifier: '123',
        inode: '123-i',
        canEdit: true,
        canRead: true,
        canSeeRules: true,
        contentType: 'htmlpageasset',
        canLock: true,
        locked: false,
        lockedBy: '',
        lockedByName: '',
        live: true
    } as unknown as DotCMSPage,
    numberContents: 6,
    viewAs: {
        language: {
            id: 1,
            language: 'English',
            countryCode: 'US',
            languageCode: '1',
            country: 'United States'
        },
        variantId: DEFAULT_VARIANT_ID,
        mode: 'LIVE',
        persona: {
            ...DEFAULT_PERSONA
        } as unknown as DotCMSViewAsPersona
    },
    site: mockSites[0] as DotCMSSite,
    layout: mockDotLayout() as DotCMSLayout,
    template: mockDotTemplate() as unknown as DotCMSTemplate,
    containers: mockDotContainers() as unknown as DotCMSPageAssetContainers
};

export const URL_CONTENT_MAP_MOCK: DotCMSURLContentMap = {
    contentType: 'Blog',
    identifier: '123',
    inode: '1234',
    title: 'hello world',
    baseType: 'CONTENT',
    folder: 'SYSTEM_FOLDER',
    host: '123',
    languageId: 1,
    live: true,
    modDate: '1722992210315',
    modUser: 'dotcms.org.1',
    owner: 'dotcms.org.1',
    url: '/content.ec123',
    working: true,
    archived: false,
    hasTitleImage: true,
    hostName: 'demo.dotcms.com',
    locked: false,
    modUserName: 'Admin User',
    sortOrder: 0,
    stInode: '799f176a-d32e-4844-a07c-1b5fcd107578',
    titleImage: 'image',
    URL_MAP_FOR_CONTENT: '/test-url',
    urlMap: 'test'
};

export const MOCK_RESPONSE_VTL: DotCMSPageAsset = {
    page: {
        pageURI: 'test-url',
        title: 'Test Page',
        identifier: '123',
        inode: '123-i',
        canEdit: true,
        canRead: true,
        canSeeRules: true,
        rendered: '<html><body><h1>Hello, World!</h1></body></html>',
        contentType: 'htmlpageasset',
        canLock: true,
        locked: false,
        lockedBy: '',
        lockedByName: '',
        live: true,
        liveInode: '1234',
        stInode: '12345'
    } as unknown as DotCMSPage,
    numberContents: 6,
    viewAs: {
        language: {
            id: 1,
            language: 'English',
            countryCode: 'US',
            languageCode: '1',
            country: 'United States'
        },
        mode: 'LIVE',
        persona: {
            ...DEFAULT_PERSONA
        } as unknown as DotCMSViewAsPersona
    },
    site: mockSites[0] as DotCMSSite,
    layout: mockDotLayout() as DotCMSLayout,
    template: mockDotTemplate() as unknown as DotCMSTemplate,
    containers: mockDotContainers() as unknown as DotCMSPageAssetContainers
};

export const PAGE_RESPONSE_URL_CONTENT_MAP = {
    ...MOCK_RESPONSE_VTL,
    urlContentMap: URL_CONTENT_MAP_MOCK
};

export const dotPageContainerStructureMock: DotCMSPageAssetContainers = {
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
            postLoop: '123',
            preLoop: '123',
            source: CONTAINER_SOURCE.DB,
            title: '123',
            type: '123',
            working: false
        } as DotCMSContainer,
        containerStructures: [
            {
                contentTypeVar: '123'
            }
        ] as DotCMSContainerStructure[],
        contentlets: {
            '123': [
                {
                    baseType: '123',
                    contentType: '123',
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
                    contentType: '456',
                    folder: '456',
                    identifier: '456',
                    inode: '456',
                    languageId: 456,
                    live: false,
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
                    baseType: '123',
                    folder: '123',
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
        contentletsId: ['123']
    },
    pageId: 'test',
    position: 'before'
};

export const PAGE_RESPONSE_BY_LANGUAGE_ID = {
    1: of({
        page: {
            title: 'hello world',
            identifier: '123',
            inode: '123',
            canEdit: true,
            canRead: true,
            canSeeRules: true,
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
            canSeeRules: true,
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
    }),

    3: of({
        page: {
            title: 'hello world',
            identifier: '123',
            inode: '123',
            canEdit: true,
            canRead: true,
            canSeeRules: true,
            pageURI: 'index',
            liveInode: '1234',
            stInode: '12345',
            live: true
        },
        viewAs: {
            language: {
                id: 3,
                languageCode: 'ES',
                countryCode: '',
                language: 'Spanish',
                country: 'Spain'
            },
            persona: DEFAULT_PERSONA
        },
        site: mockSites[0],
        template: {
            drawed: true
        }
    })
};

export const getVanityUrl = (url, mock) =>
    ({
        vanityUrl: {
            ...mock,
            url
        }
    }) as unknown as DotCMSPageAsset;

export const getNewVanityUrl = (uri: string, mock: Partial<DotCMSVanityUrl>) =>
    ({
        vanityUrl: {
            ...mock,
            uri,
            url: uri
        }
    }) as unknown as DotCMSPageAsset;

export const FORWARD_VANITY_URL = {
    pattern: '',
    vanityUrlId: '',
    url: 'test-url',
    siteId: '',
    languageId: 1,
    forwardTo: 'vanity-url',
    response: 200,
    order: 1,
    temporaryRedirect: false,
    permanentRedirect: false,
    forward: true
};

export const PERMANENT_REDIRECT_VANITY_URL = {
    pattern: '',
    vanityUrlId: '',
    url: 'test-url',
    siteId: '',
    languageId: 1,
    forwardTo: 'vanity-url',
    response: 200,
    order: 1,
    temporaryRedirect: false,
    permanentRedirect: true,
    forward: false
};

export const TEMPORARY_REDIRECT_VANITY_URL = {
    pattern: '',
    vanityUrlId: '',
    url: 'test-url',
    siteId: '',
    languageId: 1,
    forwardTo: 'vanity-url',
    response: 200,
    order: 1,
    temporaryRedirect: true,
    permanentRedirect: false,
    forward: false
};

export const NEW_TEMPORARY_REDIRECT_VANITY_URL = {
    action: 302,
    forwardTo: 'new-vanity-url',
    uri: 'test-url'
};

export const NEW_PERMANENT_REDIRECT_VANITY_URL = {
    action: 301,
    forwardTo: 'new-vanity-url',
    uri: 'test-url'
};

export const NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_RESPONSE = {
    forwardTo: 'new-vanity-url',
    uri: 'test-url',
    response: 200
};

export const NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_ACTION = {
    action: 200,
    forwardTo: 'new-vanity-url',
    uri: 'test-url'
};

export const EMA_DRAG_ITEM_CONTENTLET_MOCK: EmaDragItem = {
    baseType: 'CONTENT',
    contentType: 'kenobi',
    draggedPayload: {
        type: 'contentlet',
        item: {
            container: {
                identifier: '321',
                acceptTypes: 'kenobi,theChosenOne,yoda',
                maxContentlets: 3,
                uuid: '123'
            },
            contentlet: {
                identifier: '321',
                inode: '123',
                title: 'title',
                contentType: 'kenobi'
            }
        },
        move: true
    }
};

export const MOCK_CONTENTLET_AREA: ContentletArea = {
    x: 200,
    y: 180,
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
            uuid: ''
        },
        contentlet: {
            identifier: '123',
            inode: '',
            title: '',
            contentType: ''
        }
    }
};

export const ACTION_MOCK: ClientData = {
    container: {
        acceptTypes: 'file',
        identifier: '789',
        maxContentlets: 100,
        uuid: '2'
    }
};

export const ITEM_MOCK = {
    contentType: 'file',
    baseType: 'FILEASSET',
    draggedPayload: null
};

export const getBoundsMockWithEmptyContainer = (payload: ClientData): Container[] => {
    return [
        {
            x: 10,
            y: 10,
            width: 980,
            height: 180,
            contentlets: [],
            payload
        }
    ];
};

export const getBoundsMock = (payload: ClientData): Container[] => {
    return [
        {
            x: 10,
            y: 10,
            width: 980,
            height: 180,
            contentlets: [
                {
                    x: 20,
                    y: 20,
                    width: 940,
                    height: 140,
                    payload: null as unknown as ActionPayload
                },
                {
                    x: 40,
                    y: 20,
                    width: 940,
                    height: 140,
                    payload: null as unknown as ActionPayload
                }
            ],
            payload
        }
    ];
};

export const BOUNDS_MOCK: Container[] = getBoundsMock(ACTION_MOCK);

export const BOUNDS_EMPTY_CONTAINER_MOCK: Container[] =
    getBoundsMockWithEmptyContainer(ACTION_MOCK);

export const ACTION_PAYLOAD_MOCK: ActionPayload = {
    language_id: '1',
    pageContainers: [
        {
            identifier: 'container-identifier-123',
            uuid: 'uuid-123',
            contentletsId: ['contentlet-identifier-123']
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
        identifier: 'container-identifier-123',
        acceptTypes: 'test',
        uuid: 'uuid-123',
        maxContentlets: 1,
        contentletsId: ['123']
    },
    pageId: 'test',
    position: 'after'
};

export const BASE_SHELL_ITEMS = [
    {
        icon: 'pi-file',
        label: 'editema.editor.navbar.content',
        href: 'content',
        id: 'content'
    },
    {
        icon: 'pi-table',
        label: 'editema.editor.navbar.layout',
        href: 'layout',
        id: 'layout',
        isDisabled: false,
        tooltip: null
    },
    {
        icon: 'pi-sliders-h',
        label: 'editema.editor.navbar.rules',
        id: 'rules',
        href: `rules/${MOCK_RESPONSE_HEADLESS.page.identifier}`,
        isDisabled: false
    },
    {
        iconURL: 'experiments',
        label: 'editema.editor.navbar.experiments',
        href: `experiments/${MOCK_RESPONSE_HEADLESS.page.identifier}`,
        id: 'experiments',
        isDisabled: false
    },
    {
        icon: 'pi-th-large',
        label: 'editema.editor.navbar.page-tools',
        id: 'page-tools'
    },
    {
        icon: 'pi-ellipsis-v',
        label: 'editema.editor.navbar.properties',
        id: 'properties',
        isDisabled: false
    }
];

export const BASE_SHELL_PROPS_RESPONSE = {
    canRead: true,
    error: null,
    seoParams: {
        siteId: MOCK_RESPONSE_HEADLESS.site.identifier,
        languageId: 1,
        currentUrl: '/test-url',
        requestHostName: 'http://localhost:3000'
    },
    items: BASE_SHELL_ITEMS
};

export const UVE_PAGE_RESPONSE_MAP = {
    // URLContentMap with hasLiveVersion false
    9: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            rendered: '<div>New Content - Hello World</div>',
            canEdit: true,
            hasLiveVersion: false
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 4,
                language: 'German',
                countryCode: 'DE',
                languageCode: 'de',
                country: 'Germany'
            },
            persona: DEFAULT_PERSONA
        },
        urlContentMap: { ...URL_CONTENT_MAP_MOCK, hasLiveVersion: false },
        containers: dotPageContainerStructureMock
    }),
    // Locked without unlock permission
    8: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canEdit: true,
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            canLock: false,
            isLocked: true,
            lockedByUser: 'user'
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 2,
                language: 'Spanish',
                countryCode: 'ES',
                languageCode: 'es',
                country: 'España'
            },
            persona: DEFAULT_PERSONA
        },
        containers: dotPageContainerStructureMock
    }),
    //Locked  with unlock permission
    7: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canEdit: true,
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            canLock: true,
            locked: true,
            lockedByName: 'user'
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 2,
                language: 'Spanish',
                countryCode: 'ES',
                languageCode: 'es',
                country: 'España'
            },
            persona: DEFAULT_PERSONA
        },
        containers: dotPageContainerStructureMock
    }),
    6: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            canEdit: false
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 6,
                language: 'Portuguese',
                countryCode: 'BR',
                languageCode: 'br',
                country: 'Brazil'
            },
            persona: DEFAULT_PERSONA
        },
        urlContentMap: URL_CONTENT_MAP_MOCK,
        containers: dotPageContainerStructureMock
    }),
    5: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: 'i-have-a-running-experiment',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            rendered: '<div>New Content - Hello World</div>',
            canEdit: true
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 4,
                language: 'Russian',
                countryCode: 'Ru',
                languageCode: 'ru',
                country: 'Russia'
            },
            persona: DEFAULT_PERSONA
        },
        urlContentMap: URL_CONTENT_MAP_MOCK,
        containers: dotPageContainerStructureMock
    }),
    4: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            rendered: '<div>New Content - Hello World</div>',
            canEdit: true
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 4,
                language: 'German',
                countryCode: 'DE',
                languageCode: 'de',
                country: 'Germany'
            },
            persona: DEFAULT_PERSONA
        },
        urlContentMap: URL_CONTENT_MAP_MOCK,
        containers: dotPageContainerStructureMock
    }),
    3: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            rendered: '<div>hello world</div>',
            canEdit: true
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 3,
                language: 'German',
                countryCode: 'DE',
                languageCode: 'de',
                country: 'Germany'
            },
            persona: DEFAULT_PERSONA
        },
        urlContentMap: URL_CONTENT_MAP_MOCK,
        containers: dotPageContainerStructureMock
    }),
    2: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one',
            canEdit: true
        },
        site: {
            identifier: '123'
        },
        viewAs: {
            language: {
                id: 2,
                language: 'Spanish',
                countryCode: 'ES',
                languageCode: 'es',
                country: 'España'
            },
            persona: DEFAULT_PERSONA
        },
        containers: dotPageContainerStructureMock
    }),
    1: of({
        page: {
            title: 'hello world',
            inode: PAGE_INODE_MOCK,
            identifier: '123',
            canEdit: true,
            canRead: true,
            canSeeRules: true,
            pageURI: 'page-one'
        },
        site: {
            identifier: '123'
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
        urlContentMap: URL_CONTENT_MAP_MOCK,
        containers: dotPageContainerStructureMock
    })
};

export const PAGE_WITH_ADVANCE_RENDER_TEMPLATE_MOCK = {
    ...MOCK_RESPONSE_VTL,
    page: {
        ...MOCK_RESPONSE_VTL.page,
        rendered: '<h1>Advance template render</h1>'
    }
};

export const dotPropertiesServiceMock = {
    getFeatureFlags: () =>
        of({
            [FeaturedFlags.FEATURE_FLAG_UVE_PREVIEW_MODE]: false,
            [FeaturedFlags.FEATURE_FLAG_UVE_TOGGLE_LOCK]: false,
            [FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR]: false
        })
};

export const mockCurrentUser: CurrentUser = {
    email: 'test@example.com',
    givenName: 'Test',
    loginAs: false,
    roleId: 'role123',
    surname: 'User',
    userId: 'user123'
};
