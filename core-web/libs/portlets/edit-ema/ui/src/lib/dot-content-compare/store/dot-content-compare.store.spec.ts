import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import {
    DotContentletService,
    DotContentTypeService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { MockDotHttpErrorManagerService } from '@dotcms/utils-testing';

import { DotContentCompareStore } from './dot-content-compare.store';

const generateRandomString = function (length: number) {
    const words = ['lorem', 'ipsum', 'dolor', 'sit', 'amet', 'consectetur', 'adipiscing', 'elit'];

    return Array.from({ length }, () => words[Math.floor(Math.random() * words.length)]).join(' ');
};

const generateInode = function () {
    const hexDigits = '0123456789abcdef';
    const segments = [8, 4, 4, 4, 12];
    let uuid = '';

    segments.forEach((segmentLength, index) => {
        if (index !== 0) uuid += '-'; // Add dash between segments

        for (let i = 0; i < segmentLength; i++) {
            uuid += hexDigits.charAt(Math.floor(Math.random() * hexDigits.length));
        }
    });

    return uuid;
};

const newContentObj = function () {
    return {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }],
        contentType: 'ContentType1',
        date: 1639548000000,
        dateTime: 1639612800000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: generateInode(),
        keyValue: { keyone: generateRandomString(1), keytwo: generateRandomString(1) },
        languageId: 1,
        live: true,
        locked: false,
        modDate: 1639784363639,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639780580960,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: generateRandomString(3),
        time: 104400000,
        title: generateRandomString(3),
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: true
    };
};

const getContentTypeMOCKResponse = {
    baseType: 'CONTENT',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780027000,
            id: 'c095bd48e11e4c40dd5693ed0c28a356',
            indexed: false,
            listed: false,
            modDate: 1639780040000,
            name: 'fields-0',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 0,
            unique: false,
            variable: 'fields0'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780027000,
            id: '2a0812293dbd0f0aff9fff4d1c60e681',
            indexed: false,
            listed: false,
            modDate: 1639780040000,
            name: 'fields-1',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 1,
            unique: false,
            variable: 'fields1'
        },
        {
            categories: {
                categoryName: 'Age or Gender',
                description: null,
                inode: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
                key: 'ageOrGender',
                keywords: '',
                sortOrder: 0
            },
            clazz: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'SYSTEM',
            fieldType: 'Category',
            fieldTypeLabel: 'Category',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780040000,
            id: '67e3d75d69fa2a1d6d6fef4159c670b8',
            indexed: true,
            listed: false,
            modDate: 1639780040000,
            name: 'caategory',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            values: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
            variable: 'caategory'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'LONG_TEXT',
            fieldType: 'Key-Value',
            fieldTypeLabel: 'Key/Value',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780112000,
            id: 'd9fb5c2074b1a9ae8f66f772bbb6afb9',
            indexed: false,
            listed: false,
            modDate: 1639780114000,
            name: 'key-value',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 3,
            unique: false,
            variable: 'keyValue'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableDateField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'DATE',
            fieldType: 'Date',
            fieldTypeLabel: 'Date',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780085000,
            id: '0d8d9762b7b4026745fdd88f4bbab631',
            indexed: false,
            listed: false,
            modDate: 1639780114000,
            name: 'date',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 4,
            unique: false,
            variable: 'date'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'DATE',
            fieldType: 'Date-and-Time',
            fieldTypeLabel: 'Date and Time',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780092000,
            id: 'f538d6106aa764bf4105bc88e3e231d4',
            indexed: false,
            listed: false,
            modDate: 1639780114000,
            name: 'dateTime',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'dateTime'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'DATE',
            fieldType: 'Time',
            fieldTypeLabel: 'Time',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780098000,
            id: '1ce2c9112ccff90b9447585adb1857d9',
            indexed: false,
            listed: false,
            modDate: 1639780114000,
            name: 'time',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 6,
            unique: false,
            variable: 'time'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: '0121c052881956cd95bfe5dde968ca07',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1639780139000,
            id: '5d3ee41199505b25d989594675fc8525',
            indexed: false,
            listed: false,
            modDate: 1639780139000,
            name: 'text',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 7,
            unique: false,
            variable: 'text'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    iDate: 1639780027000,
    icon: 'event_note',
    id: '0121c052881956cd95bfe5dde968ca07',
    layout: null,
    modDate: 1639780139000,
    multilingualable: false,
    name: 'contentType',
    sortOrder: 0,
    system: false,
    systemActionMappings: {},
    variable: 'ContentType1',
    versionable: true,
    workflows: null
};

const getContentletVersionsMOCKResponse = [
    {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }, { girls: 'Girls' }],
        contentType: 'ContentType1',
        date: 1639548000000,
        dateTime: 1639612800000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
        keyValue: { Colorado: 'snow', 'Costa Rica': 'summer' },
        languageId: 1,
        live: true,
        locked: false,
        modDate: 1639784363639,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639784363639,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: 'final value',
        time: 104400000,
        title: '758cb37699eae8500d64acc16ebc468e',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: true
    },
    {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }, { girls: 'Girls' }],
        contentType: 'ContentType1',
        date: 1639548000000,
        dateTime: 1639612800000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: '0e45cf0f-83bb-4291-a66d-813f35c4f71a',
        keyValue: { 'Costa Rica': 'summer' },
        languageId: 1,
        live: false,
        locked: false,
        modDate: 1639780580960,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639780580960,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: 'Field with new value',
        time: 68400000,
        title: '758cb37699eae8500d64acc16ebc468e',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: false
    },
    {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }, { mens: 'Mens' }, { womens: 'Womens' }],
        contentType: 'ContentType1',
        date: 1638338400000,
        dateTime: 1638399600000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: '40e5d7cd-2117-47d5-b96d-3278b188deeb',
        keyValue: { keyone: 'Alajuela', keytwo: 'Heredia' },
        languageId: 1,
        live: false,
        locked: false,
        modDate: 1639780516227,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639780516227,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: 'This is a text field',
        time: 82800000,
        title: '758cb37699eae8500d64acc16ebc468e',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: false
    }
];

const expectedData = {
    data: {
        working: {
            archived: false,
            baseType: 'CONTENT',
            caategory: 'Boys,Girls',
            contentType: 'ContentType1',
            date: '12/15/2021',
            dateTime: '12/15/2021 - 06:00 PM',
            folder: 'SYSTEM_FOLDER',
            hasLiveVersion: true,
            hasTitleImage: false,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            identifier: '758cb37699eae8500d64acc16ebc468e',
            inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
            keyValue: 'Colorado: snow <br/>Costa Rica: summer <br/>',
            languageId: 1,
            live: true,
            locked: false,
            modDate: '12/17/2021 - 05:39 PM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            publishDate: 1639784363639,
            sortOrder: 0,
            stInode: '0121c052881956cd95bfe5dde968ca07',
            text: 'final value',
            time: '11:00 PM',
            title: '758cb37699eae8500d64acc16ebc468e',
            titleImage: 'TITLE_IMAGE_NOT_FOUND',
            url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
            working: true
        },
        compare: {
            archived: false,
            baseType: 'CONTENT',
            caategory: 'Boys,Girls',
            contentType: 'ContentType1',
            date: '12/15/2021',
            dateTime: '12/15/2021 - 06:00 PM',
            folder: 'SYSTEM_FOLDER',
            hasLiveVersion: true,
            hasTitleImage: false,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            identifier: '758cb37699eae8500d64acc16ebc468e',
            inode: '0e45cf0f-83bb-4291-a66d-813f35c4f71a',
            keyValue: 'Costa Rica: summer <br/>',
            languageId: 1,
            live: false,
            locked: false,
            modDate: '12/17/2021 - 04:36 PM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            publishDate: 1639780580960,
            sortOrder: 0,
            stInode: '0121c052881956cd95bfe5dde968ca07',
            text: 'Field with new value',
            time: '01:00 PM',
            title: '758cb37699eae8500d64acc16ebc468e',
            titleImage: 'TITLE_IMAGE_NOT_FOUND',
            url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
            working: false
        },
        versions: [
            {
                archived: false,
                baseType: 'CONTENT',
                caategory: 'Boys,Girls',
                contentType: 'ContentType1',
                date: '12/15/2021',
                dateTime: '12/15/2021 - 06:00 PM',
                folder: 'SYSTEM_FOLDER',
                hasLiveVersion: true,
                hasTitleImage: false,
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                hostName: 'demo.dotcms.com',
                identifier: '758cb37699eae8500d64acc16ebc468e',
                inode: '0e45cf0f-83bb-4291-a66d-813f35c4f71a',
                keyValue: 'Costa Rica: summer <br/>',
                languageId: 1,
                live: false,
                locked: false,
                modDate: '12/17/2021 - 04:36 PM',
                modUser: 'dotcms.org.1',
                modUserName: 'Admin User',
                owner: 'dotcms.org.1',
                publishDate: 1639780580960,
                sortOrder: 0,
                stInode: '0121c052881956cd95bfe5dde968ca07',
                text: 'Field with new value',
                time: '01:00 PM',
                title: '758cb37699eae8500d64acc16ebc468e',
                titleImage: 'TITLE_IMAGE_NOT_FOUND',
                url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
                working: false
            },
            {
                archived: false,
                baseType: 'CONTENT',
                caategory: 'Boys,Mens,Womens',
                contentType: 'ContentType1',
                date: '12/01/2021',
                dateTime: '12/01/2021 - 05:00 PM',
                folder: 'SYSTEM_FOLDER',
                hasLiveVersion: true,
                hasTitleImage: false,
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                hostName: 'demo.dotcms.com',
                identifier: '758cb37699eae8500d64acc16ebc468e',
                inode: '40e5d7cd-2117-47d5-b96d-3278b188deeb',
                keyValue: 'keyone: Alajuela <br/>keytwo: Heredia <br/>',
                languageId: 1,
                live: false,
                locked: false,
                modDate: '12/17/2021 - 04:35 PM',
                modUser: 'dotcms.org.1',
                modUserName: 'Admin User',
                owner: 'dotcms.org.1',
                publishDate: 1639780516227,
                sortOrder: 0,
                stInode: '0121c052881956cd95bfe5dde968ca07',
                text: 'This is a text field',
                time: '05:00 PM',
                title: '758cb37699eae8500d64acc16ebc468e',
                titleImage: 'TITLE_IMAGE_NOT_FOUND',
                url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
                working: false
            }
        ],
        fields: [
            {
                categories: {
                    categoryName: 'Age or Gender',
                    description: null,
                    inode: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
                    key: 'ageOrGender',
                    keywords: '',
                    sortOrder: 0
                },
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'SYSTEM',
                fieldType: 'Category',
                fieldTypeLabel: 'Category',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780040000,
                id: '67e3d75d69fa2a1d6d6fef4159c670b8',
                indexed: true,
                listed: false,
                modDate: 1639780040000,
                name: 'caategory',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 2,
                unique: false,
                values: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
                variable: 'caategory'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'LONG_TEXT',
                fieldType: 'Key-Value',
                fieldTypeLabel: 'Key/Value',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780112000,
                id: 'd9fb5c2074b1a9ae8f66f772bbb6afb9',
                indexed: false,
                listed: false,
                modDate: 1639780114000,
                name: 'key-value',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 3,
                unique: false,
                variable: 'keyValue'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'DATE',
                fieldType: 'Date',
                fieldTypeLabel: 'Date',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780085000,
                id: '0d8d9762b7b4026745fdd88f4bbab631',
                indexed: false,
                listed: false,
                modDate: 1639780114000,
                name: 'date',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 4,
                unique: false,
                variable: 'date'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'DATE',
                fieldType: 'Date-and-Time',
                fieldTypeLabel: 'Date and Time',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780092000,
                id: 'f538d6106aa764bf4105bc88e3e231d4',
                indexed: false,
                listed: false,
                modDate: 1639780114000,
                name: 'dateTime',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 5,
                unique: false,
                variable: 'dateTime'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'DATE',
                fieldType: 'Time',
                fieldTypeLabel: 'Time',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780098000,
                id: '1ce2c9112ccff90b9447585adb1857d9',
                indexed: false,
                listed: false,
                modDate: 1639780114000,
                name: 'time',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 6,
                unique: false,
                variable: 'time'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                contentTypeId: '0121c052881956cd95bfe5dde968ca07',
                dataType: 'TEXT',
                fieldType: 'Text',
                fieldTypeLabel: 'Text',
                fieldVariables: [],
                fixed: false,
                iDate: 1639780139000,
                id: '5d3ee41199505b25d989594675fc8525',
                indexed: false,
                listed: false,
                modDate: 1639780139000,
                name: 'text',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 7,
                unique: false,
                variable: 'text'
            }
        ]
    },
    showDiff: true
};

const newCompare = {
    archived: false,
    baseType: 'CONTENT',
    caategory: 'Boys,Mens,Womens',
    contentType: 'ContentType1',
    date: '12/01/2021',
    dateTime: '12/01/2021 - 05:00 PM',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: false,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    identifier: '758cb37699eae8500d64acc16ebc468e',
    inode: '40e5d7cd-2117-47d5-b96d-3278b188deeb',
    keyValue: 'keyone: Alajuela <br/>keytwo: Heredia <br/>',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '12/17/2021 - 04:35 PM',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    publishDate: 1639780516227,
    sortOrder: 0,
    stInode: '0121c052881956cd95bfe5dde968ca07',
    text: 'This is a text field',
    time: '05:00 PM',
    title: '758cb37699eae8500d64acc16ebc468e',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
    working: false
};

describe('DotContentCompareStore', () => {
    let dotContentCompareStore: DotContentCompareStore;
    for (let index = 0; index < 21; index++) {
        getContentletVersionsMOCKResponse.push(newContentObj());
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotContentCompareStore,
                DotHttpErrorManagerService,
                {
                    provide: DotContentTypeService,
                    useValue: {
                        getContentType: () => of(getContentTypeMOCKResponse)
                    }
                },
                {
                    provide: DotContentletService,
                    useValue: {
                        getContentletVersions: () => of(getContentletVersionsMOCKResponse)
                    }
                },
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'America/Costa_Rica',
                                label: 'Central Standard Time (America/Costa_Rica)',
                                offset: -21600000
                            })
                    }
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                },
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                }
            ]
        });
        dotContentCompareStore = TestBed.inject(DotContentCompareStore);
    });

    it('should load initial data correctly', (done) => {
        dotContentCompareStore.loadData({
            inode: '0e45cf0f-83bb-4291-a66d-813f35c4f71a',
            identifier: '758cb37699eae8500d64acc16ebc468e',
            language: 'en'
        });
        dotContentCompareStore.vm$.subscribe((data) => {
            expect(data).toEqual(expectedData);
            done();
        });
    });

    it('should update compare', () => {
        dotContentCompareStore.updateCompare(expectedData.data.versions[1]);
        dotContentCompareStore.state$.subscribe((data) => {
            expect(data.data.compare).toEqual(newCompare);
        });
    });

    it('should update showDiff', () => {
        dotContentCompareStore.updateShowDiff(false);
        dotContentCompareStore.state$.subscribe((data) => {
            expect(data.showDiff).toEqual(false);
        });
    });
});
