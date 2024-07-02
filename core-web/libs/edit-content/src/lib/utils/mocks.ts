import {
    AsyncValidator,
    FormControl,
    FormGroup,
    FormGroupDirective,
    Validator,
    ValidatorFn
} from '@angular/forms';

import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { FIELD_TYPES } from '../models/dot-edit-content-field.enum';
import { EditContentPayload } from '../models/dot-edit-content-form.interface';
import {
    CustomTreeNode,
    TreeNodeItem
} from '../models/dot-edit-content-host-folder-field.interface';

/* FIELDS MOCK BY TYPE */
export const TEXT_FIELD_MOCK: DotCMSContentTypeField = {
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
};

export const TEXT_AREA_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'LONG_TEXT',
    defaultValue: 'Some value',
    fieldType: 'Textarea',
    fieldTypeLabel: 'Textarea',
    fieldVariables: [],
    fixed: false,
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
};

export const SELECT_FIELD_TEXT_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    defaultValue: '123-ad',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697579843000,
    hint: 'A hint Text',
    id: 'a6f33b8941b6c06c8ab36e44c4bf6500',
    indexed: false,
    listed: false,
    modDate: 1697661626000,
    name: 'selectNormal',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    values: 'Option 1|Test,1\r\nOption 2|2\r\nOption 3|3\r\n123-ad\r\nrules and weird code',
    variable: 'selectNormal'
};

export const SELECT_FIELD_BOOLEAN_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'BOOL',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint Text',
    forceIncludeInApi: false,
    iDate: 1697661273000,
    id: '8c5648fe4dedc06baf314f362c00431b',
    indexed: false,
    listed: false,
    modDate: 1697661626000,
    name: 'selectBoolean',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 4,
    unique: false,
    values: 'Truthy|true\r\nFalsy|false',
    variable: 'selectBoolean'
};

export const SELECT_FIELD_FLOAT_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'FLOAT',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    hint: 'A hint Text',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697661848000,
    id: '8c2edc3ee461fa50041a9e5831f1a86a',
    indexed: false,
    listed: false,
    modDate: 1697661848000,
    name: 'selectDecimal',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 5,
    unique: false,
    values: 'One hundred point five|100.5\r\nThree point five|10.3',
    variable: 'selectDecimal'
};

export const SELECT_FIELD_INTEGER_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'INTEGER',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697662296000,
    id: '89bdd8e525ef9a4c923f4b54d9a0e4f8',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'selectWholeNumber',
    readOnly: false,
    required: false,
    searchable: false,
    hint: 'A hint Text',
    sortOrder: 6,
    unique: false,
    values: 'One hundred|100\r\nOne thousand|1000\r\nTen thousand|10000',
    variable: 'selectWholeNumber'
};

export const RADIO_FIELD_TEXT_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697598313000,
    id: '824b4e9907fe4f450ced438598cc0ce8',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radio',
    readOnly: false,
    required: false,
    hint: 'A hint Text',
    searchable: false,
    sortOrder: 8,
    unique: false,
    values: 'One|one\r\nTwo|two',
    variable: 'radio'
};

export const RADIO_FIELD_BOOLEAN_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'BOOL',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697656862000,
    id: 'e4b3ef6a8cb50ff77fe2534c2b237d71',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radioTrueFalse',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 9,
    hint: 'A hint Text',
    unique: false,
    values: 'Falsy|false\r\nTruthy|true',
    variable: 'radioTrueFalse'
};

export const RADIO_FIELD_FLOAT_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'FLOAT',
    defaultValue: '9.3',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697656895000,
    id: 'b26138321e5a449cdf7b73f927643016',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radioDecimal',
    readOnly: false,
    hint: 'A hint Text',
    required: false,
    searchable: false,
    sortOrder: 10,
    unique: false,
    values: 'Five point two|5.2\r\nNine point three|9.3',
    variable: 'radioDecimal'
};

export const RADIO_FIELD_INTEGER_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'INTEGER',
    defaultValue: '30',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697656956000,
    id: 'bdd0f00375e23b7a64608d78c8fcb2dc',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radioWholeNumber',
    readOnly: false,
    hint: 'A hint Text',
    required: true,
    searchable: false,
    sortOrder: 11,
    unique: false,
    values: 'Twelve|12\r\nTwenty|20\r\nThirty|30',
    variable: 'radioWholeNumber'
};

export const DATE_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableDateField',
    contentTypeId: '4d22214338844b4aed0367933e9bf500',
    dataType: 'DATE',
    fieldType: 'Date',
    fieldTypeLabel: 'Date',
    fieldVariables: [],
    fixed: false,
    iDate: 1698250833000,
    id: '55b4fdcf51eddf01b0f462384e8b3439',
    indexed: false,
    hint: 'A hint text',
    listed: false,
    modDate: 1698250833000,
    name: 'date',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    variable: 'date'
};

export const DATE_AND_TIME_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
    contentTypeId: '4d22214338844b4aed0367933e9bf500',
    dataType: 'DATE',
    defaultValue: 'now',
    fieldType: 'Date-and-Time',
    fieldTypeLabel: 'Date and Time',
    fieldVariables: [],
    fixed: false,
    iDate: 1698250840000,
    id: '9e669bacc84ce6530bba5f295becc76c',
    indexed: false,
    listed: false,
    hint: 'A hint text',
    modDate: 1698250840000,
    name: 'date and time',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    variable: 'dateAndTime'
};

export const TIME_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
    contentTypeId: '4d22214338844b4aed0367933e9bf500',
    dataType: 'DATE',
    fieldType: 'Time',
    fieldTypeLabel: 'Time',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698250847000,
    id: '1005cde03b962dd0ce7bb4c4ec97f89c',
    indexed: false,
    listed: false,
    modDate: 1698250847000,
    name: 'time',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 4,
    unique: false,
    variable: 'time'
};

export const TAG_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTagField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'SYSTEM',
    defaultValue: 'some, tags, separated, by, comma',
    fieldType: 'Tag',
    fieldTypeLabel: 'Tag',
    fieldVariables: [],
    fixed: false,
    hint: 'Some hint',
    iDate: 1698346136000,
    id: '1ba4927b83aae5b17921679053b0b5fe',
    indexed: true,
    listed: false,
    modDate: 1698346136000,
    name: 'Some tag',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    variable: 'someTag'
};

export const CHECKBOX_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    contentTypeId: '93ebaff75f3e3887bea73eca04588dc9',
    dataType: 'TEXT',
    fieldType: 'Checkbox',
    fieldTypeLabel: 'Checkbox',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497ce3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'check',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    values: 'one|one\r\ntwo|two',
    variable: 'check'
};

export const MULTI_SELECT_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    contentTypeId: '93ebaff75f3e3887bea73eca04588dc9',
    dataType: 'LONG_TEXT',
    fieldType: 'Multi-Select',
    fieldTypeLabel: 'Multi Select',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698264695000,
    id: '535a6de288e3fe91fad2679e8d7d966b',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'multiSelect',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    values: 'one|one\r\ntwo|two',
    variable: 'multiSelect'
};

export const BLOCK_EDITOR_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    contentTypeId: '799f176a-d32e-4844-a07c-1b5fcd107578',
    dataType: 'LONG_TEXT',
    fieldType: 'Story-Block',
    fieldTypeLabel: 'Block Editor',
    fieldVariables: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '71fe962eb681c5ffd6cd1623e5fc575a',
            id: 'b19e1d5d-47ad-40d7-b2bf-ccd0a5a86590',
            key: 'contentTypes',
            value: 'Activity,CallToAction,calendarEvent,Product,Destination'
        }
    ],
    fixed: false,
    iDate: 1649791703000,
    id: '71fe962eb681c5ffd6cd1623e5fc575a',
    indexed: false,
    listed: false,
    hint: 'A helper text',
    modDate: 1699364930000,
    name: 'Blog Content',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 13,
    unique: false,
    variable: 'blogContent'
};

export const BINARY_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
    contentTypeId: '93ebaff75f3e3887bea73eca04588dc9',
    dataType: 'BINARY',
    fieldType: 'Binary',
    fieldTypeLabel: 'Binary Field',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698264695000,
    id: '535a6de288e3fe91fad2679e8d7d966b',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'Binary',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    values: '/test.png',
    variable: 'Binary'
};

export const CUSTOM_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'LONG_TEXT',
    fieldType: 'Custom-Field',
    fieldTypeLabel: 'Custom Field',
    fieldVariables: [],
    fixed: false,
    iDate: 1700516848000,
    id: '64d5c84f04df900c79a94e087c6fed05',
    indexed: false,
    listed: false,
    modDate: 1700622670000,
    hint: 'A hint text',
    name: 'custom',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 4,
    unique: false,
    values: '<script>\r\nfunction searchClicked() {\r\n    console.log("Yoo")\r\n    form.get(\'select\').setValue("three");\r\n    form.get(\'title\').setValue("From Dojo!");\r\n}\r\n</script>\r\n\r\n<button dojoType="dijit.form.Button" onClick="searchClicked()" iconClass="searchIcon">Search</button>',
    variable: 'custom'
};

export const JSON_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'JSON-Field',
    fieldTypeLabel: 'jsonField',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497cd3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'json',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    values: '{ "test": "test" }',
    variable: 'json'
};

export const KEY_VALUE_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'Key-Value',
    fieldTypeLabel: 'KeyValue',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497cd3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'KeyValue',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    values: '{ "key1": "value1" }',
    variable: 'KeyValue'
};

export const WYSIWYG_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableWYSIWYGField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'WYSIWYG',
    fieldTypeLabel: 'WYSIWYG',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497cd3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'WYSIWYG',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    values: '<p>HELLO</p>',
    variable: 'WYSIWYG'
};

export const HOST_FOLDER_TEXT_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'SYSTEM',
    fieldType: 'Host-Folder',
    fieldTypeLabel: 'Site or Folder',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    forceIncludeInApi: false,
    iDate: 1717083750000,
    id: 'b7c41ffd6b6bc1250f2fc85a3637471b',
    indexed: true,
    listed: false,
    modDate: 1717088310000,
    name: 'Site Or Folder',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    variable: 'siteOrFolder'
};

export const CATEGORY_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableWYSIWYGField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'Category',
    fieldTypeLabel: 'Category',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497cd3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'Category',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    values: '<p>HELLO</p>',
    variable: 'Category'
};

export const FIELDS_MOCK: DotCMSContentTypeField[] = [
    TEXT_FIELD_MOCK,
    TEXT_AREA_FIELD_MOCK,
    SELECT_FIELD_TEXT_MOCK,
    SELECT_FIELD_BOOLEAN_MOCK,
    SELECT_FIELD_FLOAT_MOCK,
    SELECT_FIELD_INTEGER_MOCK,
    RADIO_FIELD_TEXT_MOCK,
    RADIO_FIELD_BOOLEAN_MOCK,
    RADIO_FIELD_FLOAT_MOCK,
    RADIO_FIELD_INTEGER_MOCK,
    DATE_FIELD_MOCK,
    DATE_AND_TIME_FIELD_MOCK,
    TIME_FIELD_MOCK,
    TAG_FIELD_MOCK,
    CHECKBOX_FIELD_MOCK,
    MULTI_SELECT_FIELD_MOCK,
    BLOCK_EDITOR_FIELD_MOCK,
    BINARY_FIELD_MOCK,
    CUSTOM_FIELD_MOCK,
    JSON_FIELD_MOCK,
    KEY_VALUE_MOCK,
    WYSIWYG_MOCK,
    HOST_FOLDER_TEXT_MOCK,
    CATEGORY_MOCK
];

export const FIELD_MOCK: DotCMSContentTypeField = TEXT_FIELD_MOCK;

export const BINARY_FIELD_CONTENTLET: DotCMSContentlet = {
    binaryField:
        '/dA/39de8193694d96c2a6bab783ba9c85b5/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
    publishDate: '2023-11-07 16:49:24.787',
    inode: 'd135b73a-8c8f-42ce-bd4e-deb3c067cedd',
    BinaryContentAsset: '39de8193694d96c2a6bab783ba9c85b5/binaryField',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    locked: false,
    stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
    contentType: 'Binary',
    BinaryMetaData: {
        modDate: 1699375764242,
        sha256: '7b4e1c307518ea00e503469e690e4abe42fe1b13aef43cbcbf6eafd9aa532057',
        length: 136168,
        title: 'Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        version: 20220201,
        isImage: true,
        fileSize: 136168,
        name: 'Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        width: 645,
        contentType: 'image/png',
        height: 547
    },
    identifier: '39de8193694d96c2a6bab783ba9c85b5',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: true,
    sortOrder: 0,
    hostName: 'demo.dotcms.com',
    modDate: '2023-11-07 16:49:24.787',
    title: '39de8193694d96c2a6bab783ba9c85b5',
    baseType: 'CONTENT',
    archived: false,
    working: true,
    live: true,
    owner: 'dotcms.org.1',
    languageId: 1,
    url: '/content.d135b73a-8c8f-42ce-bd4e-deb3c067cedd',
    titleImage: 'binaryField',
    modUserName: 'Admin User',
    hasLiveVersion: true,
    modUser: 'dotcms.org.1',
    binaryFieldVersion:
        '/dA/d135b73a-8c8f-42ce-bd4e-deb3c067cedd/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
    __icon__: 'contentIcon',
    contentTypeIcon: 'event_note',
    variant: 'DEFAULT',
    value: '/dA/39de8193694d96c2a6bab783ba9c85b5/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png'
};

/* HELPER FUNCTIONS */

// This creates a mock FormGroup from an array of fielda
export const createFormControlObjectMock = (fields = FIELDS_MOCK) => {
    return fields.reduce((acc, field) => {
        acc[field.variable] = new FormControl(null);

        return acc;
    }, {});
};

// Create a mock FormGroupDirective
export const createFormGroupDirectiveMock = (
    formGroup: FormGroup = FORM_GROUP_MOCK,
    validator: (Validator | ValidatorFn)[] = [],
    asyncValidators: AsyncValidator[] = []
) => {
    const formGroupDirectiveMock = new FormGroupDirective(validator, asyncValidators);

    formGroupDirectiveMock.form = formGroup;

    return formGroupDirectiveMock;
};

function getAllFields(data: DotCMSContentTypeLayoutRow[]) {
    let fields = [];

    data.forEach((row) => {
        row.columns.forEach((column) => {
            fields = [...fields, ...column.fields];
        });
    });

    return fields;
}

/* CONSTANTS */

export const DOT_MESSAGE_SERVICE_MOCK = new MockDotMessageService({});

export const CALENDAR_FIELD_TYPES = [FIELD_TYPES.DATE, FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME];

/* LAYOUT/FORM MOCKS */

// This creates a mock FormGroup from an array of fielda
export const FORM_GROUP_MOCK = new FormGroup(createFormControlObjectMock());

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
                        variable: 'text2',
                        regexCheck:
                            '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
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
                    },
                    TAG_FIELD_MOCK,
                    DATE_FIELD_MOCK
                ]
            }
        ]
    }
];

export const TAB_SINGLE_ROW_MOCK: DotCMSContentTypeLayoutRow = {
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
    columns: []
};

export const TAB_DIVIDER_MOCK: DotCMSContentTypeLayoutRow = {
    divider: {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
        dataType: 'SYSTEM',
        fieldType: 'Tab_divider',
        fieldTypeLabel: 'Tab_divider',
        fieldVariables: [],
        fixed: false,
        iDate: 1697051073000,
        id: 'a31ea895f80eb0a3754e4a2292e09a52',
        indexed: false,
        listed: false,
        modDate: 1697051077000,
        name: 'New Tab',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 0,
        unique: false,
        variable: 'tab'
    },
    columns: []
};

export const MULTIPLE_TABS_MOCK: DotCMSContentTypeLayoutRow[] = [
    TAB_SINGLE_ROW_MOCK,
    TAB_DIVIDER_MOCK
];

export const MOCK_DATE = 1699990073562;

export const JUST_FIELDS_MOCKS = getAllFields(LAYOUT_MOCK);

export const LAYOUT_FIELDS_VALUES_MOCK = {
    name1: 'Placeholder', // This is the default value of the name1 field
    text2: null,
    text3: null,
    someTag: 'some,tags,separated,by,comma', // This is the default value of the tag field
    date: '2023-11-14 19:27:53'
};

const metadata = {};
metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = false;

export const CONTENT_FORM_DATA_MOCK: EditContentPayload = {
    actions: [],
    contentType: {
        metadata,
        layout: LAYOUT_MOCK,
        fields: JUST_FIELDS_MOCKS,
        contentType: 'Test'
    } as unknown as DotCMSContentType,
    contentlet: {
        // This contentlet is some random mock, if you need you can change the properties
        date: MOCK_DATE, // To add the value to the date field, defaultValue is string and I don't think we should change the whole type just for this
        publishDate: '2023-11-07 16:49:24.787',
        inode: 'd135b73a-8c8f-42ce-bd4e-deb3c067cedd',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        locked: false,
        stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
        contentType: 'Binary',
        identifier: '39de8193694d96c2a6bab783ba9c85b5',
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        hostName: 'demo.dotcms.com',
        modDate: '2023-11-07 16:49:24.787',
        title: '39de8193694d96c2a6bab783ba9c85b5',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        live: true,
        owner: 'dotcms.org.1',
        languageId: 1,
        url: '/content.d135b73a-8c8f-42ce-bd4e-deb3c067cedd',
        titleImage: 'binaryField',
        modUserName: 'Admin User',
        hasLiveVersion: true,
        modUser: 'dotcms.org.1',
        binaryFieldVersion:
            '/dA/d135b73a-8c8f-42ce-bd4e-deb3c067cedd/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        __icon__: 'contentIcon',
        contentTypeIcon: 'event_note',
        variant: 'DEFAULT'
    },
    loading: false
};

/* CONTENT TYPE MOCKS */

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

export const MockResizeObserver = class {
    constructor() {
        //
    }

    observe() {
        //
    }

    unobserve() {
        //
    }

    disconnect() {
        //
    }
};

export const TREE_SELECT_SITES_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    },
    {
        key: 'System Host',
        label: 'System Host',
        data: {
            hostname: 'System Host',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    }
];

export const TREE_SELECT_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                key: 'demo.dotcms.comlevel1',
                label: 'demo.dotcms.com/level1/',
                data: {
                    hostname: 'demo.dotcms.com',
                    path: '/level1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        key: 'demo.dotcms.comlevel1child1',
                        label: 'demo.dotcms.com/level1/child1/',
                        data: {
                            hostname: 'demo.dotcms.com',
                            path: '/level1/child1/',
                            type: 'folder'
                        },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder'
                    }
                ]
            },
            {
                key: 'demo.dotcms.comlevel2',
                label: 'demo.dotcms.com/level2/',
                data: {
                    hostname: 'demo.dotcms.com',
                    path: '/level2/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder'
            }
        ]
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    }
];

export const TREE_SELECT_MOCK_NODE: CustomTreeNode = {
    node: { ...TREE_SELECT_MOCK[0].children[0] },
    tree: {
        path: 'demo.dotcms.com',
        folders: [...TREE_SELECT_MOCK[0].children]
    }
};
