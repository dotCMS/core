import {
    FormGroup,
    FormControl,
    FormGroupDirective,
    AsyncValidator,
    ValidatorFn,
    Validator
} from '@angular/forms';

import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';

export const FIELDS_MOCK = [
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
        dataType: 'TEXT',
        fieldType: 'Text',
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        iDate: 1696896882000,
        id: 'c3b928bc2b59fc22c67022de4dd4b5c4',
        indexed: false,
        listed: false,
        hint: 'A helper text',
        modDate: 1696896882000,
        name: 'testVariable',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 2,
        unique: false,
        variable: 'testVariable'
    },
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
        contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
        dataType: 'LONG_TEXT',
        defaultValue: 'Some value',
        fieldType: 'Textarea',
        fieldTypeLabel: 'Textarea',
        fieldVariables: [],
        fixed: false,
        forceIncludeInApi: false,
        hint: 'Some hint',
        iDate: 1697553818000,
        id: '950c7ddbbe59996386330316a32cccc4',
        indexed: false,
        listed: false,
        modDate: 1697554437000,
        name: 'some text area',
        readOnly: false,
        required: true,
        searchable: false,
        sortOrder: 2,
        unique: false,
        variable: 'someTextArea'
    }
];

export const FIELD_MOCK: DotCMSContentTypeField = FIELDS_MOCK[0];

// This creates a mock FormGroup from an array of fielda
export const createFormControlObjectMock = (fields = FIELDS_MOCK) => {
    return fields.reduce((acc, field) => {
        acc[field.variable] = new FormControl('');

        return acc;
    }, {});
};

export const FORM_GROUP_MOCK = new FormGroup(createFormControlObjectMock());

// Create a mock FormGroupDirective
export const createFormGroupDirectiveMock = (
    validator: (Validator | ValidatorFn)[] = [],
    asyncValidators: AsyncValidator[] = []
) => {
    const formGroupDirectiveMock = new FormGroupDirective(validator, asyncValidators);

    formGroupDirectiveMock.form = FORM_GROUP_MOCK;

    return formGroupDirectiveMock;
};

export const LAYOUT_MOCK: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'a31ea895f80eb0a3754e4a2292e09a52',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
            name: 'fields-0',
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
                    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                    dataType: 'SYSTEM',
                    fieldType: 'Column',
                    fieldTypeLabel: 'Column',
                    fieldVariables: [],
                    fixed: false,
                    iDate: 1697051073000,
                    id: 'd4c32b4b9fb5b11c58c245d4a02bef47',
                    indexed: false,
                    listed: false,
                    modDate: 1697051077000,
                    name: 'fields-1',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 1,
                    unique: false,
                    variable: 'fields1'
                },
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        defaultValue: 'Placeholder',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        hint: 'A hint Text',
                        iDate: 1697051093000,
                        id: '1d1505a4569681b923769acb785fd093',
                        indexed: false,
                        listed: false,
                        modDate: 1697051093000,
                        name: 'name1',
                        readOnly: false,
                        required: true,
                        searchable: false,
                        sortOrder: 2,
                        unique: false,
                        variable: 'name1'
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        iDate: 1697051107000,
                        id: 'fc776c45044f2d043f5e98eaae36c9ff',
                        indexed: false,
                        listed: false,
                        modDate: 1697051107000,
                        name: 'text2',
                        readOnly: false,
                        required: true,
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
                    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                    dataType: 'SYSTEM',
                    fieldType: 'Column',
                    fieldTypeLabel: 'Column',
                    fieldVariables: [],
                    fixed: false,
                    iDate: 1697051077000,
                    id: '848fc78a11e7290efad66eb39333ae2b',
                    indexed: false,
                    listed: false,
                    modDate: 1697051107000,
                    name: 'fields-2',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 4,
                    unique: false,
                    variable: 'fields2'
                },
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        hint: 'A hint text2',
                        iDate: 1697051118000,
                        id: '1f6765de8d4ad069ff308bfca56b9255',
                        indexed: false,
                        listed: false,
                        modDate: 1697051118000,
                        name: 'text3',
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
];

export const CONTENT_TYPE_MOCK: DotCMSContentType = {
    baseType: 'CONTENT',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'a31ea895f80eb0a3754e4a2292e09a52',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
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
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'd4c32b4b9fb5b11c58c245d4a02bef47',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
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
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            defaultValue: 'Placeholder',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            hint: 'A hint Text 2',
            iDate: 1697051093002,
            id: '1d1505a4569681b923769acb785fd094',
            indexed: false,
            listed: false,
            modDate: 1697051093000,
            name: 'name13',
            readOnly: false,
            required: true,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'name13'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051107001,
            id: 'fc776c45044f2d043f5e98eaae36c9f2',
            indexed: false,
            listed: false,
            modDate: 1697051107000,
            name: 'text23',
            readOnly: false,
            required: true,
            searchable: false,
            sortOrder: 3,
            unique: false,
            variable: 'text23'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051077000,
            id: '848fc78a11e7290efad66eb39333ae2b',
            indexed: false,
            listed: false,
            modDate: 1697051107000,
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
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            hint: 'A hint text2',
            iDate: 1697051118000,
            id: '1f6765de8d4ad069ff308bfca56b9255',
            indexed: false,
            listed: false,
            modDate: 1697051118000,
            name: 'text3',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'text3'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    iDate: 1697051073000,
    icon: 'event_note',
    id: 'd46d6404125ac27e6ab68fad09266241',
    layout: LAYOUT_MOCK,
    modDate: 1697051118000,
    multilingualable: false,
    name: 'Test',
    contentType: 'Test',
    system: false,
    systemActionMappings: {},
    variable: 'Test',
    versionable: true,
    workflows: [
        {
            archived: false,
            creationDate: new Date(1697047303976),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1697047292887),
            name: 'System Workflow',
            system: true
        }
    ],
    nEntries: 0
};
