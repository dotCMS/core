// ContentType 1 tab, with 1 row and 2 columns
// 1st column has 1 text field required and 1 text field with hint
// 2nd column has 1 text field with default value
// +---------------------------------------------------+
// |                   Row                             |
// +-------------------+-------------------------------+
// |      Column 1     |       Column 2                |
// +-------------------+-------------------------------+
// | Text1 (required)  |                               |
// |                   |                               |
// | Text2 (with hint) |        Text3                  |
// |                   |        (with default value)   |
// +-------------------+-------------------------------+

import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSWorkflowAction,
    DotCMSWorkflowStatus
} from '@dotcms/dotcms-models';

export const MOCK_CONTENTTYPE_1_TAB: DotCMSContentType = {
    baseType: 'CONTENT',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012258000,
            id: '5f992a3eb0d9b92fec736475c4bfb183',
            indexed: false,
            listed: false,
            modDate: 1729012263000,
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
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012258000,
            id: 'b0b0c33b9abf64ecccf51f7900b9af19',
            indexed: false,
            listed: false,
            modDate: 1729012263000,
            name: 'fields-1',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 1,
            unique: false,
            variable: 'fields1'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012281000,
            id: '69b2ccbb36a0efc135db107eb882d74e',
            indexed: false,
            listed: false,
            modDate: 1729012362000,
            name: 'text1',
            readOnly: false,
            required: true,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'text1'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            hint: 'text2 hint',
            iDate: 1729012287000,
            id: '4fb628337f5e27ff96ff6ad320d7952b',
            indexed: false,
            listed: false,
            modDate: 1729012373000,
            name: 'text2',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 3,
            unique: false,
            variable: 'text2'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012263000,
            id: '683e570c1fe299628ea10639f354c725',
            indexed: false,
            listed: false,
            modDate: 1729012287000,
            name: 'fields-2',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 4,
            unique: false,
            variable: 'fields2'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'TEXT',
            defaultValue: 'default value',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012357000,
            id: 'b2d546ae37278b9bb717078be5522a1e',
            indexed: false,
            listed: false,
            modDate: 1729012357000,
            name: 'text3',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'text3'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
            contentTypeId: 'bec7fa8325253216d9ee23a0693baa17',
            dataType: 'LONG_TEXT',
            fieldType: 'Multi-Select',
            fieldTypeLabel: 'Multi Select',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729012357000,
            id: 'c3817bbc0b605578daca363ba752a3a3',
            indexed: false,
            listed: false,
            modDate: 1729012357000,
            name: 'Multi select',
            values: 'A\r\nB\r\nC',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'multiselect'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',

    icon: 'event_note',
    iDate: 1729012258000,
    id: '196c8d303e265143806ad19356406ae3',
    layout: [
        {
            divider: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                contentTypeId: '196c8d303e265143806ad19356406ae3',
                dataType: 'SYSTEM',
                fieldContentTypeProperties: [],
                fieldType: 'Row',
                fieldTypeLabel: 'Row',
                fieldVariables: [],
                fixed: false,
                listed: false,
                modDate: 1729012263000,
                name: 'fields-0',
                readOnly: false,
                required: false,
                forceIncludeInApi: false,
                iDate: 1729012258000,
                id: '5f992a3eb0d9b92fec736475c4bfb183',
                indexed: false,

                searchable: false,
                sortOrder: 0,
                unique: false,
                variable: 'fields0'
            },
            columns: [
                {
                    columnDivider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        contentTypeId: '196c8d303e265143806ad19356406ae3',
                        dataType: 'SYSTEM',
                        fieldContentTypeProperties: [],
                        fieldType: 'Column',
                        fieldTypeLabel: 'Column',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1729012258000,
                        id: 'b0b0c33b9abf64ecccf51f7900b9af19',
                        indexed: false,
                        listed: false,
                        modDate: 1729012263000,

                        searchable: false,
                        sortOrder: 1,
                        unique: false,
                        name: 'fields-1',
                        readOnly: false,
                        required: false,
                        variable: 'fields1'
                    },
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            id: '69b2ccbb36a0efc135db107eb882d74e',
                            indexed: false,
                            listed: false,
                            modDate: 1729012362000,
                            name: 'text1',
                            readOnly: false,
                            required: true,
                            searchable: false,
                            sortOrder: 2,

                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1729012281000,
                            unique: false,
                            variable: 'text1'
                        },
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            dataType: 'TEXT',
                            modDate: 1729012373000,
                            name: 'text2',
                            readOnly: false,
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            hint: 'text2 hint',
                            iDate: 1729012287000,
                            id: '4fb628337f5e27ff96ff6ad320d7952b',
                            indexed: false,
                            listed: false,

                            required: false,
                            searchable: false,
                            sortOrder: 3,
                            unique: false,
                            variable: 'text2'
                        }
                    ]
                },
                {
                    columnDivider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        contentTypeId: '196c8d303e265143806ad19356406ae3',
                        dataType: 'SYSTEM',
                        fieldContentTypeProperties: [],

                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1729012263000,
                        id: '683e570c1fe299628ea10639f354c725',
                        indexed: false,
                        listed: false,
                        modDate: 1729012287000,
                        name: 'fields-2',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        fieldType: 'Column',
                        fieldTypeLabel: 'Column',
                        sortOrder: 4,
                        unique: false,
                        variable: 'fields2'
                    },
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            listed: false,
                            modDate: 1729012357000,
                            name: 'text3',
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1729012357000,
                            id: 'b2d546ae37278b9bb717078be5522a1e',
                            indexed: false,

                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 5,
                            unique: false,
                            variable: 'text3'
                        }
                    ]
                }
            ]
        }
    ],
    metadata: {
        CONTENT_EDITOR2_ENABLED: false
    },
    modDate: 1729012373000,
    systemActionMappings: {},
    variable: 'TestMock',
    versionable: true,
    multilingualable: false,
    name: 'test Mock',
    system: false,

    workflows: [
        {
            archived: false,
            creationDate: new Date(1729012304196),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1728492941334),
            name: 'System Workflow',
            system: true
        }
    ],
    nEntries: 0
};

// Mock of actions of a new ContentType
export const MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB: DotCMSWorkflowAction[] = [
    {
        actionInputs: [],
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
        metadata: null,
        name: 'Save',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
        nextStepCurrentStep: false,
        order: 0,
        owner: null,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['NEW', 'EDITING', 'LOCKED', 'PUBLISHED', 'UNPUBLISHED']
    },
    {
        actionInputs: [],
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'b9d89c80-3d88-4311-8365-187323c96436',
        metadata: null,
        name: 'Publish',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        nextStepCurrentStep: false,
        order: 0,
        owner: null,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LISTING', 'UNLOCKED', 'NEW', 'EDITING', 'LOCKED', 'PUBLISHED', 'UNPUBLISHED']
    }
];

// AKA contentlet
// Contain the values saved from a specific contenttype
export const MOCK_CONTENTLET_1_TAB: DotCMSContentlet = {
    archived: false,
    baseType: 'CONTENT',
    contentType: 'TestMock',
    creationDate: '1729016573151',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: false,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    identifier: '2978bb3b66e372b1ffffa9376f33c37b',
    inode: 'cc120e84-ae80-49d8-9473-36d183d0c1c9',
    languageId: 1,
    live: true,
    locked: false,
    modDate: '1729024086645',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    ownerUserName: 'Admin User',
    publishDate: '1729024086674',
    publishUser: 'dotcms.org.1',
    publishUserName: 'Admin User',
    sortOrder: 0,
    stInode: '196c8d303e265143806ad19356406ae3',
    // Start content of the form
    text1: 'content text 1',
    text2: 'content text 2',
    text3: 'default value modified',
    multiselect: 'A,B,C', // stored selected options
    text11: 'Tab 2 input content', // input in the second tab
    // end content of the form
    title: '2978bb3b66e372b1ffffa9376f33c37b',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: '/content.7af6259d-51b0-4c49-9e08-67cbc74100d6',
    working: true,
    disabledWYSIWYG: ['wysiwygField1', 'wysiwygField2'] // WYSIWYG fields disabled for code editor
};

// Mock contentlet without disabledWYSIWYG for testing new content scenarios
export const MOCK_CONTENTLET_WITHOUT_DISABLED_WYSIWYG: DotCMSContentlet = {
    ...MOCK_CONTENTLET_1_TAB,
    disabledWYSIWYG: undefined
};

// ContentType with 2 tabs
// Tab 1: 1 row and 2 columns
// 1st column has 1 text field required and 1 text field with hint
// 2nd column has 1 text field with default value
// Tab 2: 1 row with 1 column containing 1 text field
// +---------------------------------------------------+
// |  Tab 1                |  Tab 2                    |
// +---------------------------------------------------+
// |        Row            |        Row                |
// +----------+------------+---------------------------+
// | Column 1 | Column 2   |      Column 1             |
// +----------+------------+---------------------------+
// | Text1    |            |                           |
// |(required)|            |                           |
// |          |   Text3    |       Text1               |
// | Text2    |  (default  |                           |
// | (hint)   |   value)   |                           |
// +----------+------------+---------------------------+
export const MOCK_CONTENTTYPE_2_TABS: DotCMSContentType = {
    baseType: 'CONTENT',
    defaultType: false,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    fields: [
        {
            variable: 'fields1',
            unique: false,
            sortOrder: 1,
            searchable: false,
            required: false,
            readOnly: false,
            fieldType: 'Column',
            fieldContentTypeProperties: [],
            dataType: 'SYSTEM',
            name: 'fields-1',
            modDate: 1729012263000,
            listed: false,
            indexed: false,
            id: 'b0b0c33b9abf64ecccf51f7900b9af19',
            iDate: 1729012258000,
            forceIncludeInApi: false,
            fixed: false,
            fieldVariables: [],
            fieldTypeLabel: 'Column',

            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
        },
        {
            variable: 'fields0',
            unique: false,
            fieldVariables: [],
            fieldTypeLabel: 'Row',
            fieldType: 'Row',
            fieldContentTypeProperties: [],
            sortOrder: 0,
            searchable: false,
            required: false,
            readOnly: false,
            name: 'fields-0',
            modDate: 1729012263000,
            listed: false,
            indexed: false,
            id: '5f992a3eb0d9b92fec736475c4bfb183',
            iDate: 1729012258000,
            forceIncludeInApi: false,
            fixed: false,

            dataType: 'SYSTEM',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
        },
        {
            variable: 'text1',
            unique: false,
            forceIncludeInApi: false,
            fixed: false,
            fieldVariables: [],
            sortOrder: 2,
            searchable: false,
            required: true,
            readOnly: false,
            name: 'text1',
            modDate: 1729012362000,
            listed: false,
            indexed: false,
            id: '69b2ccbb36a0efc135db107eb882d74e',
            iDate: 1729012281000,

            fieldTypeLabel: 'Text',
            fieldType: 'Text',
            dataType: 'TEXT',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField'
        },
        {
            variable: 'text2',
            unique: false,
            sortOrder: 3,
            searchable: false,
            required: false,
            iDate: 1729012287000,
            forceIncludeInApi: false,
            fixed: false,
            fieldVariables: [],
            fieldTypeLabel: 'Text',
            fieldType: 'Text',
            readOnly: false,
            name: 'text2',
            modDate: 1729012373000,
            listed: false,
            indexed: false,
            id: '4fb628337f5e27ff96ff6ad320d7952b',
            hint: 'text2 hint',

            dataType: 'TEXT',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField'
        },
        {
            variable: 'fields2',
            unique: false,
            sortOrder: 4,
            iDate: 1729012263000,
            forceIncludeInApi: false,
            fixed: false,
            fieldVariables: [],
            searchable: false,
            required: false,
            readOnly: false,
            name: 'fields-2',
            modDate: 1729012287000,
            listed: false,
            indexed: false,
            id: '683e570c1fe299628ea10639f354c725',

            fieldTypeLabel: 'Column',
            fieldType: 'Column',
            fieldContentTypeProperties: [],
            dataType: 'SYSTEM',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
        },
        {
            variable: 'text3',
            unique: false,
            sortOrder: 5,
            searchable: false,
            fixed: false,
            fieldVariables: [],
            fieldTypeLabel: 'Text',
            fieldType: 'Text',
            defaultValue: 'default value',
            required: false,
            readOnly: false,
            name: 'text3',
            modDate: 1729023673000,
            listed: false,
            indexed: false,
            id: 'b2d546ae37278b9bb717078be5522a1e',
            iDate: 1729012357000,
            forceIncludeInApi: false,

            dataType: 'TEXT',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            listed: false,
            modDate: 1729025952000,
            name: 'New Tab',
            readOnly: false,
            required: false,
            searchable: false,
            dataType: 'SYSTEM',
            fieldType: 'Tab_divider',
            fieldTypeLabel: 'Tab Divider',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729025952000,
            id: '263d6fa59a75fe9fc33ca929903c34fe',
            indexed: false,

            sortOrder: 6,
            unique: false,
            variable: 'newTab'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'SYSTEM',
            fieldContentTypeProperties: [],
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729025973000,
            id: 'b699c53c74fedd58e866627a09a1f3b7',
            indexed: false,
            listed: false,

            searchable: false,
            sortOrder: 7,
            unique: false,
            modDate: 1729025973000,
            name: 'fields-3',
            readOnly: false,
            required: false,
            variable: 'fields3'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'SYSTEM',
            listed: false,
            modDate: 1729025973000,
            name: 'fields-4',
            readOnly: false,
            fieldContentTypeProperties: [],
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1729025973000,
            id: '76c04624b88dbb58207a4a40bbac4bca',
            indexed: false,

            required: false,
            searchable: false,
            sortOrder: 8,
            unique: false,
            variable: 'fields4'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: '196c8d303e265143806ad19356406ae3',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            modDate: 1729025973000,
            name: 'Text 1',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 9,
            iDate: 1729025973000,
            id: '883619ed3cea50a1ed4cdd76366a4b3c',
            indexed: false,
            listed: false,

            unique: false,
            variable: 'text11'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
            contentTypeId: 'bec7fa8325253216d9ee23a0693baa17',
            dataType: 'LONG_TEXT',
            fieldType: 'Multi-Select',
            fieldTypeLabel: 'Multi Select',
            fieldVariables: [],
            fixed: false,
            forceIncludeInApi: false,
            iDate: 1735844530000,
            id: 'c3817bbc0b605578daca363ba752a3a3',
            indexed: false,
            listed: false,
            modDate: 1735844530000,
            name: 'Multi select',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            values: 'A\r\nB\r\nC', // this are the options of the multiselect field
            variable: 'multiselect'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',

    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    icon: 'event_note',
    iDate: 1729012258000,

    id: '196c8d303e265143806ad19356406ae3',
    layout: [
        {
            divider: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                contentTypeId: '196c8d303e265143806ad19356406ae3',
                dataType: 'SYSTEM',
                fieldContentTypeProperties: [],
                fieldType: 'Row',
                indexed: false,
                listed: false,
                modDate: 1729012263000,
                name: 'fields-0',
                fieldTypeLabel: 'Row',
                fieldVariables: [],
                fixed: false,
                forceIncludeInApi: false,
                iDate: 1729012258000,
                id: '5f992a3eb0d9b92fec736475c4bfb183',

                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 0,
                unique: false,
                variable: 'fields0'
            },
            columns: [
                {
                    columnDivider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        contentTypeId: '196c8d303e265143806ad19356406ae3',
                        dataType: 'SYSTEM',
                        fieldContentTypeProperties: [],
                        fieldType: 'Column',
                        fieldTypeLabel: 'Column',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        name: 'fields-1',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 1,
                        unique: false,
                        iDate: 1729012258000,
                        id: 'b0b0c33b9abf64ecccf51f7900b9af19',
                        indexed: false,
                        listed: false,
                        modDate: 1729012263000,

                        variable: 'fields1'
                    },
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            listed: false,
                            modDate: 1729012362000,
                            name: 'text1',
                            readOnly: false,
                            required: true,
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1729012281000,
                            id: '69b2ccbb36a0efc135db107eb882d74e',
                            indexed: false,

                            searchable: false,
                            sortOrder: 2,
                            unique: false,
                            variable: 'text1'
                        },
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',

                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            hint: 'text2 hint',
                            iDate: 1729012287000,
                            id: '4fb628337f5e27ff96ff6ad320d7952b',
                            indexed: false,
                            listed: false,
                            modDate: 1729012373000,
                            name: 'text2',
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 3,
                            unique: false,
                            variable: 'text2'
                        }
                    ]
                },
                {
                    columnDivider: {
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1729012263000,
                        id: '683e570c1fe299628ea10639f354c725',
                        indexed: false,
                        listed: false,
                        modDate: 1729012287000,
                        name: 'fields-2',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 4,
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        contentTypeId: '196c8d303e265143806ad19356406ae3',
                        dataType: 'SYSTEM',
                        fieldContentTypeProperties: [],
                        fieldType: 'Column',
                        fieldTypeLabel: 'Column',
                        fieldVariables: [],

                        unique: false,
                        variable: 'fields2'
                    },
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            dataType: 'TEXT',
                            forceIncludeInApi: false,
                            iDate: 1729012357000,
                            id: 'b2d546ae37278b9bb717078be5522a1e',
                            indexed: false,
                            listed: false,
                            modDate: 1729023673000,
                            name: 'text3',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            defaultValue: 'default value',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            fixed: false,

                            sortOrder: 5,
                            unique: false,
                            variable: 'text3'
                        }
                    ]
                }
            ]
        },
        {
            divider: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                contentTypeId: '196c8d303e265143806ad19356406ae3',
                dataType: 'SYSTEM',
                fieldType: 'Tab_divider',
                fieldTypeLabel: 'Tab Divider',
                fieldVariables: [],
                fixed: false,
                forceIncludeInApi: false,
                iDate: 1729025952000,
                id: '263d6fa59a75fe9fc33ca929903c34fe',
                indexed: false,
                listed: false,
                modDate: 1729025952000,
                name: 'New Tab',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 6,
                unique: false,
                variable: 'newTab'
            },
            columns: []
        },
        {
            divider: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                contentTypeId: '196c8d303e265143806ad19356406ae3',
                dataType: 'SYSTEM',
                fieldContentTypeProperties: [],
                fieldType: 'Row',
                fieldTypeLabel: 'Row',
                fieldVariables: [],
                fixed: false,
                forceIncludeInApi: false,
                iDate: 1729025973000,
                id: 'b699c53c74fedd58e866627a09a1f3b7',
                indexed: false,
                listed: false,
                modDate: 1729025973000,
                name: 'fields-3',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 7,
                unique: false,
                variable: 'fields3'
            },
            columns: [
                {
                    columnDivider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        contentTypeId: '196c8d303e265143806ad19356406ae3',
                        dataType: 'SYSTEM',
                        fieldContentTypeProperties: [],
                        fieldType: 'Column',
                        fieldTypeLabel: 'Column',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1729025973000,
                        id: '76c04624b88dbb58207a4a40bbac4bca',
                        indexed: false,
                        listed: false,
                        modDate: 1729025973000,
                        name: 'fields-4',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 8,
                        unique: false,
                        variable: 'fields4'
                    },
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            contentTypeId: '196c8d303e265143806ad19356406ae3',
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1729025973000,
                            id: '883619ed3cea50a1ed4cdd76366a4b3c',
                            indexed: false,
                            listed: false,
                            modDate: 1729025973000,
                            name: 'Text 1',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 9,
                            unique: false,
                            variable: 'text11'
                        }
                    ]
                }
            ]
        }
    ],
    metadata: {
        CONTENT_EDITOR2_ENABLED: true
    },
    modDate: 1729025973000,
    multilingualable: false,
    name: 'test Mock',
    system: false,
    systemActionMappings: {},
    variable: 'TestMock',
    versionable: true,
    workflows: [
        {
            archived: false,
            creationDate: new Date(1729012842268),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1728492941334),
            name: 'System Workflow',
            system: true
        }
    ],
    nEntries: 0
};

/**
 * Fields allowed to be used in form controls from the MOCK_CONTENTTYPE_2_TABS Content Type.
 *
 * This array contains a subset of fields from MOCK_CONTENTTYPE_2_TABS that are suitable
 * for form controls. It excludes fields of types defined in NON_FORM_CONTROL_FIELD_TYPES,
 * such as Row, Column, Tab_divider, Constant-Field, and Hidden-Field.

 *
 * @see MOCK_CONTENTTYPE_2_TABS
 * @see NON_FORM_CONTROL_FIELD_TYPES
 */
export const MOCK_FORM_CONTROL_FIELDS: DotCMSContentTypeField[] = [
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        sortOrder: 2,
        unique: false,
        variable: 'text1',
        contentTypeId: '196c8d303e265143806ad19356406ae3',
        dataType: 'TEXT',
        fieldType: 'Text',
        fieldTypeLabel: 'Text',
        id: '69b2ccbb36a0efc135db107eb882d74e',
        indexed: false,
        listed: false,
        modDate: 1729012362000,
        name: 'text1',
        readOnly: false,
        required: true,
        searchable: false,
        fieldVariables: [],
        fixed: false,
        forceIncludeInApi: false,
        iDate: 1729012281000
    },
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        contentTypeId: '196c8d303e265143806ad19356406ae3',
        dataType: 'TEXT',
        fieldType: 'Text',
        indexed: false,
        listed: false,
        modDate: 1729012373000,
        name: 'text2',
        readOnly: false,
        required: false,
        searchable: false,
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        forceIncludeInApi: false,
        hint: 'text2 hint',
        iDate: 1729012287000,
        id: '4fb628337f5e27ff96ff6ad320d7952b',
        sortOrder: 3,
        unique: false,
        variable: 'text2'
    },

    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        contentTypeId: '196c8d303e265143806ad19356406ae3',
        dataType: 'TEXT',
        defaultValue: 'default value',
        fieldType: 'Text',
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        forceIncludeInApi: false,
        iDate: 1729012357000,
        id: 'b2d546ae37278b9bb717078be5522a1e',
        indexed: false,
        listed: false,
        modDate: 1729023673000,
        name: 'text3',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 5,
        unique: false,
        variable: 'text3'
    },

    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        contentTypeId: '196c8d303e265143806ad19356406ae3',
        dataType: 'TEXT',
        unique: false,
        fieldType: 'Text',
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        iDate: 1729025973000,
        id: '883619ed3cea50a1ed4cdd76366a4b3c',
        indexed: false,
        listed: false,
        modDate: 1729025973000,
        name: 'Text 1',
        readOnly: false,
        forceIncludeInApi: false,
        required: false,
        searchable: false,
        sortOrder: 9,

        variable: 'text11'
    },

    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
        contentTypeId: 'bec7fa8325253216d9ee23a0693baa17',
        dataType: 'LONG_TEXT',
        fieldType: 'Multi-Select',
        fieldTypeLabel: 'Multi Select',
        fieldVariables: [],
        fixed: false,
        forceIncludeInApi: false,
        iDate: 1735844530000,
        id: 'c3817bbc0b605578daca363ba752a3a3',
        indexed: false,
        listed: false,
        modDate: 1735844530000,
        name: 'Multi select',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 2,
        unique: false,
        values: 'A\r\nB\r\nC', // this are the options of the multiselect field
        variable: 'multiselect'
    }
];

/**
 * Mock data for workflows.
 *
 * @see MOCK_WORKFLOW_DATA
 */
export const MOCK_WORKFLOW_DATA = [
    {
        action: {
            assignable: false,
            commentable: false,
            condition: '',
            actionInputs: [],
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: true,
            hasUnarchiveActionlet: false,
            hasUnpublishActionlet: false,
            icon: 'workflowIcon',
            id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
            metadata: null,
            name: 'Save',
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
            nextStepCurrentStep: false,
            order: 0,
            owner: null,
            roleHierarchyForAssign: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            showOn: ['EDITING', 'UNPUBLISHED', 'NEW', 'LOCKED', 'PUBLISHED']
        },
        firstStep: {
            creationDate: 1731595862064,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '6cb7e3bd-1710-4eed-8838-d3db60f78f19',
            myOrder: 0,
            name: 'New',
            resolved: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
        },
        scheme: {
            archived: false,
            creationDate: new Date(1731432900580),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1730906400422),
            name: 'System Workflow',
            system: true,
            variableName: 'SystemWorkflow'
        }
    },
    {
        action: {
            assignable: false,
            commentable: false,
            condition: '',
            actionInputs: [],
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: true,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: true,
            hasUnarchiveActionlet: true,
            hasUnpublishActionlet: false,
            icon: 'workflowIcon',
            id: '89685558-1449-4928-9cff-adda8648d54d',
            metadata: null,
            name: 'Save and Publish',
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
            nextStepCurrentStep: false,
            order: 0,
            owner: null,
            roleHierarchyForAssign: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            showOn: ['EDITING', 'UNPUBLISHED', 'NEW', 'LOCKED', 'LISTING', 'PUBLISHED']
        },
        firstStep: {
            creationDate: 1731595862064,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '6cb7e3bd-1710-4eed-8838-d3db60f78f19',
            myOrder: 0,
            name: 'Edit',
            resolved: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
        },
        scheme: {
            archived: false,
            creationDate: new Date(1731432900580),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: new Date(1730906400420),
            name: 'Blogs',
            system: false,
            variableName: 'Blogs'
        }
    }
];

/**
 * Mock data for workflow status.
 *
 * @see MOCK_WORKFLOW_DATA
 */
export const MOCK_WORKFLOW_STATUS: DotCMSWorkflowStatus = {
    scheme: {
        ...MOCK_WORKFLOW_DATA[1].scheme
    },
    firstStep: MOCK_WORKFLOW_DATA[1].firstStep,
    step: {
        creationDate: 1731983051341,
        enableEscalation: false,
        escalationAction: null,
        escalationTime: 0,
        id: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
        myOrder: 2,
        name: 'Published',
        resolved: false,
        schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
    },
    task: {
        assignedTo: 'Admin User',
        belongsTo: null,
        createdBy: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
        creationDate: 1731983076219,
        description: null,
        dueDate: null,
        id: 'ea337da7-25d9-494a-b4ab-03c7bcda39dc',
        inode: 'ea337da7-25d9-494a-b4ab-03c7bcda39dc',
        languageId: 1,
        modDate: 1731983076219,
        new: false,
        status: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
        title: 'asdasd',
        webasset: '93dda60a-3a10-49e3-bef4-b663d57d86df'
    }
};
