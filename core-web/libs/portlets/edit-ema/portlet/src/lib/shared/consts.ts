import { InjectionToken } from '@angular/core';

import { DotPersona } from '@dotcms/dotcms-models';

import { ActionPayload } from './models';

export const EDIT_CONTENTLET_URL =
    '/c/portal/layout?p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=';

export const ADD_CONTENTLET_URL = `/html/ng-contentlet-selector.jsp?ng=true&container_id=*CONTAINER_ID*&add=*BASE_TYPES*&language_id=*LANGUAGE_ID*`;

export const HOST = 'http://localhost:3000';

export const WINDOW = new InjectionToken<Window>('WindowToken');

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
