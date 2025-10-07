import {
    AsyncValidator,
    FormControl,
    FormGroup,
    FormGroupDirective,
    Validator,
    ValidatorFn
} from '@angular/forms';

import {
    DotCMSClazzes,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSTempFile,
    DotCMSWorkflowStatus,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { WYSIWYG_MOCK } from '../fields/dot-edit-content-wysiwyg-field/mocks/dot-edit-content-wysiwyg-field.mock';
import { DISABLED_WYSIWYG_FIELD } from '../models/disabledWYSIWYG.constant';
import { FIELD_TYPES } from '../models/dot-edit-content-field.enum';
import { DotFormData } from '../models/dot-edit-content-form.interface';
import {
    CustomTreeNode,
    TreeNodeItem
} from '../models/dot-edit-content-host-folder-field.interface';
import { DotWorkflowState } from '../models/dot-edit-content.model';

/* FIELDS MOCK BY TYPE */
export const TEXT_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
    dataType: 'TEXT',
    fieldType: 'Text',
    fieldTypeLabel: 'Text',
    fieldVariables: [],
    fixed: false,
    hint: 'A helper text',
    iDate: 1696896882000,
    id: 'c3b928bc2b59fc22c67022de4dd4b5c4',
    indexed: false,
    listed: false,
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

export const SELECT_FIELD_TEXT_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    defaultValue: '123-ad',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
    iDate: 1697579843000,
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

export const SELECT_FIELD_BOOLEAN_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'BOOL',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
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

export const SELECT_FIELD_FLOAT_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'FLOAT',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
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

export const SELECT_FIELD_INTEGER_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'INTEGER',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
    iDate: 1697662296000,
    id: '89bdd8e525ef9a4c923f4b54d9a0e4f8',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'selectWholeNumber',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 6,
    unique: false,
    values: 'One hundred|100\r\nOne thousand|1000\r\nTen thousand|10000',
    variable: 'selectWholeNumber'
};

export const RADIO_FIELD_TEXT_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
    iDate: 1697598313000,
    id: '824b4e9907fe4f450ced438598cc0ce8',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radio',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 8,
    unique: false,
    values: 'One|one\r\nTwo|two',
    variable: 'radio'
};

export const RADIO_FIELD_BOOLEAN_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'BOOL',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
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
    unique: false,
    values: 'Falsy|false\r\nTruthy|true',
    variable: 'radioTrueFalse'
};

export const RADIO_FIELD_FLOAT_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'FLOAT',
    defaultValue: '9.3',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
    iDate: 1697656895000,
    id: 'b26138321e5a449cdf7b73f927643016',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radioDecimal',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 10,
    unique: false,
    values: 'Five point two|5.2\r\nNine point three|9.3',
    variable: 'radioDecimal'
};

export const RADIO_FIELD_INTEGER_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'INTEGER',
    defaultValue: '30',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'A hint Text',
    iDate: 1697656956000,
    id: 'bdd0f00375e23b7a64608d78c8fcb2dc',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radioWholeNumber',
    readOnly: false,
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
    hint: 'A hint text',
    iDate: 1698250833000,
    id: '55b4fdcf51eddf01b0f462384e8b3439',
    indexed: false,
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
    hint: 'A hint text',
    iDate: 1698250840000,
    id: '9e669bacc84ce6530bba5f295becc76c',
    indexed: false,
    listed: false,
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
    id: '1005cde03b962dd0ce7bb4c4ec97f89c',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
    contentTypeId: '4d22214338844b4aed0367933e9bf500',
    dataType: 'DATE',
    fieldType: 'Time',
    fieldTypeLabel: 'Time',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698250847000,
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
    id: '1ba4927b83aae5b17921679053b0b5fe',
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
    id: '96909fa20a00497ce3b766b52edac0ec',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    contentTypeId: '93ebaff75f3e3887bea73eca04588dc9',
    dataType: 'TEXT',
    fieldType: 'Checkbox',
    fieldTypeLabel: 'Checkbox',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
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
    id: '535a6de288e3fe91fad2679e8d7d966b',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    contentTypeId: '93ebaff75f3e3887bea73eca04588dc9',
    dataType: 'LONG_TEXT',
    fieldType: 'Multi-Select',
    fieldTypeLabel: 'Multi Select',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698264695000,
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
    id: '71fe962eb681c5ffd6cd1623e5fc575a',
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
    id: '5df3f8fc49177c195740bcdc02ec2db7',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
    contentTypeId: 'd1901a41d38b6686dd5ed8f910346d7a',
    dataType: 'SYSTEM',
    fieldType: 'Binary',
    fieldTypeLabel: 'Binary',
    fieldVariables: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
            id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
            key: 'accept',
            value: 'image/*,.html,.ts'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
            id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
            key: 'maxFileSize',
            value: '50000'
        }
    ],
    hint: 'Helper label to be displayed below the field',
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1698153564000,
    indexed: false,
    listed: false,
    modDate: 1698153564000,
    name: 'Binary Field',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    variable: 'binaryField'
};

export const IMAGE_FIELD_MOCK: DotCMSContentTypeField = {
    id: 'fec3e11696cf9b0f99139c160a598e02',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableImageField',
    contentTypeId: 'a8f941d835e4b4f3e4e71b45add34c60',
    dataType: 'TEXT',
    fieldType: 'Image',
    fieldTypeLabel: 'Image',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1726517012000,
    indexed: false,
    listed: false,
    modDate: 1726517012000,
    name: 'Image Field',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    variable: 'imageField',
    hint: 'Helper label to be displayed below the field'
};

export const FILE_FIELD_MOCK: DotCMSContentTypeField = {
    id: 'f90afb1384e04507ba03e8701f7e4000',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableFileField',
    contentTypeId: 'a8f941d835e4b4f3e4e71b45add34c60',
    dataType: 'TEXT',
    fieldType: 'File',
    fieldTypeLabel: 'File',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1726507692000,
    indexed: false,
    listed: false,
    modDate: 1726517016000,
    name: 'File Field',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    variable: 'file1',
    hint: 'Helper label to be displayed below the field'
};

export const RELATIONSHIP_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
    contentTypeId: 'd68af52828a53805a1716e68cd902560',
    dataType: 'SYSTEM',
    fieldType: 'Relationship',
    fieldTypeLabel: 'Relationships Field',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1732655273000,
    id: '306fe444c1ae1fd063c02d8812903fc9',
    indexed: true,
    listed: false,
    modDate: 1732660181000,
    name: 'Relationship Field',
    readOnly: false,
    relationships: {
        cardinality: 0,
        isParentField: true,
        velocityVar: 'AllTypes'
    },
    required: false,
    searchable: false,
    skipRelationshipCreation: false,
    sortOrder: 6,
    unique: false,
    variable: 'relationshipField',
    hint: 'Helper label to be displayed below the field'
};

export const CUSTOM_FIELD_MOCK: DotCMSContentTypeField = {
    id: '64d5c84f04df900c79a94e087c6fed05',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'LONG_TEXT',
    fieldType: 'Custom-Field',
    fieldTypeLabel: 'Custom Field',
    fieldVariables: [],
    fixed: false,
    iDate: 1700516848000,
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
    id: '96909fa20a00497cd3b766b52edac0ec',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'JSON-Field',
    fieldTypeLabel: 'jsonField',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
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
    id: '96909fa20a00497cd3b766b52edac0ec',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'Key-Value',
    fieldTypeLabel: 'KeyValue',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
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

export const HOST_FOLDER_TEXT_MOCK: DotCMSContentTypeField = {
    id: 'b7c41ffd6b6bc1250f2fc85a3637471b',
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
    id: '96909fa20a00497cd3b766b52edac0ec',
    clazz: DotCMSClazzes.CATEGORY,
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'Category',
    fieldTypeLabel: 'Category',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
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

export const CONSTANT_FIELD_MOCK: DotCMSContentTypeField = {
    id: '666817fcecef5c10cb520c1866baa411',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableConstantField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'SYSTEM',
    fieldType: 'Constant-Field',
    fieldTypeLabel: 'Constant Field',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    hint: 'this is a hint',
    iDate: 1725491385000,
    indexed: false,
    listed: false,
    modDate: 1725491385000,
    name: 'constant',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    values: 'constant-value',
    variable: 'constant'
};

export const HIDDEN_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableHiddenField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'SYSTEM',
    fieldType: 'Hidden-Field',
    fieldTypeLabel: 'Hidden Field',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1725498554000,
    id: '22b95419744e89549bb7f2b662f5d312',
    indexed: false,
    listed: false,
    modDate: 1725499601000,
    name: 'hidden',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 5,
    unique: false,
    values: 'hidden-value',
    variable: 'hidden'
};

export const LINE_DIVIDER_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
    contentTypeId: '799f176a-d32e-4844-a07c-1b5fcd107578',
    dataType: 'SYSTEM',
    fieldType: 'Line_divider',
    fieldTypeLabel: 'Line Divider',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1685464291000,
    id: '0cc62946e4889894f3fe83a18a8bdffa',
    indexed: false,
    listed: false,
    modDate: 1735915174000,
    name: 'Open Graph (OG Meta Tags)',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 27,
    unique: false,
    variable: 'openGraph'
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
    FILE_FIELD_MOCK,
    IMAGE_FIELD_MOCK,
    CUSTOM_FIELD_MOCK,
    JSON_FIELD_MOCK,
    KEY_VALUE_MOCK,
    WYSIWYG_MOCK,
    HOST_FOLDER_TEXT_MOCK,
    CATEGORY_MOCK,
    CONSTANT_FIELD_MOCK,
    HIDDEN_FIELD_MOCK,
    RELATIONSHIP_FIELD_MOCK,
    LINE_DIVIDER_MOCK
];

export const FIELD_MOCK: DotCMSContentTypeField = TEXT_FIELD_MOCK;

export const BINARY_FIELD_CONTENTLET: DotCMSContentlet = {
    archived: false,
    baseType: 'CONTENT',
    binaryField:
        '/dA/39de8193694d96c2a6bab783ba9c85b5/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
    binaryFieldVersion:
        '/dA/d135b73a-8c8f-42ce-bd4e-deb3c067cedd/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
    BinaryContentAsset: '39de8193694d96c2a6bab783ba9c85b5/binaryField',
    BinaryMetaData: {
        contentType: 'image/png',
        fileSize: 136168,
        height: 547,
        isImage: true,
        length: 136168,
        modDate: 1699375764242,
        name: 'Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        sha256: '7b4e1c307518ea00e503469e690e4abe42fe1b13aef43cbcbf6eafd9aa532057',
        title: 'Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        version: 20220201,
        width: 645
    },
    contentType: 'Binary',
    contentTypeIcon: 'event_note',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: true,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    __icon__: 'contentIcon',
    identifier: '39de8193694d96c2a6bab783ba9c85b5',
    inode: 'd135b73a-8c8f-42ce-bd4e-deb3c067cedd',
    languageId: 1,
    live: true,
    locked: false,
    modDate: '2023-11-07 16:49:24.787',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    publishDate: '2023-11-07 16:49:24.787',
    sortOrder: 0,
    stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
    title: '39de8193694d96c2a6bab783ba9c85b5',
    titleImage: 'binaryField',
    url: '/content.d135b73a-8c8f-42ce-bd4e-deb3c067cedd',
    value: '/dA/39de8193694d96c2a6bab783ba9c85b5/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
    variant: 'DEFAULT',
    working: true
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
export const FORM_GROUP_MOCK = new FormGroup({
    ...createFormControlObjectMock(),
    [DISABLED_WYSIWYG_FIELD]: new FormControl([])
});

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
            forceIncludeInApi: false,
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
                    forceIncludeInApi: false,
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
                        forceIncludeInApi: false,
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
                        forceIncludeInApi: false,
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
                    forceIncludeInApi: false,
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
                        forceIncludeInApi: false,
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
        forceIncludeInApi: false,
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
        forceIncludeInApi: false,
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

export const CONTENT_FORM_DATA_MOCK: DotFormData = {
    contentType: {
        metadata,
        layout: LAYOUT_MOCK,
        fields: JUST_FIELDS_MOCKS,
        contentType: 'Test'
    } as unknown as DotCMSContentType,
    contentlet: {
        // This contentlet is some random mock, if you need you can change the properties
        archived: false,
        baseType: 'CONTENT',
        binaryFieldVersion:
            '/dA/d135b73a-8c8f-42ce-bd4e-deb3c067cedd/binaryField/Screenshot 2023-11-03 at 11.53.40â\u0080¯AM.png',
        contentType: 'Binary',
        contentTypeIcon: 'event_note',
        date: MOCK_DATE, // To add the value to the date field, defaultValue is string and I don't think we should change the whole type just for this
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: true,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        __icon__: 'contentIcon',
        identifier: '39de8193694d96c2a6bab783ba9c85b5',
        inode: 'd135b73a-8c8f-42ce-bd4e-deb3c067cedd',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '2023-11-07 16:49:24.787',
        modUser: 'dotcms.org.1',
        stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
        sortOrder: 0,
        title: '39de8193694d96c2a6bab783ba9c85b5',

        working: true,
        owner: 'dotcms.org.1',

        url: '/content.d135b73a-8c8f-42ce-bd4e-deb3c067cedd',
        titleImage: 'binaryField',
        modUserName: 'Admin User',

        variant: 'DEFAULT'
    },
    tabs: []
};

export const TABS_MOCK = [
    {
        title: 'Content',
        layout: CONTENT_FORM_DATA_MOCK.contentType.layout
    }
];

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
    nEntries: 0,
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
};

export const TREE_SELECT_SITES_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            id: 'demo.dotcms.com',
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
            id: 'nico.dotcms.com',
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
            id: 'System Host',
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
            id: 'demo.dotcms.com',
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
                    id: 'demo.dotcms.comlevel1',
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
                            id: 'demo.dotcms.comlevel1child1',
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
                    id: 'demo.dotcms.comlevel2',
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
            id: 'nico.dotcms.com',
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

export const NEW_FILE_MOCK: { entity: DotCMSContentlet } = {
    entity: {
        AUTO_ASSIGN_WORKFLOW: false,
        __IS_NEW_CONTENT__: true,
        __icon__: 'Icon',
        archived: false,
        asset: '/dA/a991ddc5-39dc-4782-bc04-f4c4fa0ccff6/asset/image 2.jpg',
        assetContentAsset: 'a991ddc5-39dc-4782-bc04-f4c4fa0ccff6/asset',
        assetMetaData: {
            contentType: 'image/jpeg',
            editableAsText: false,
            fileSize: 3878653,
            height: 1536,
            isImage: true,
            length: 3878653,
            modDate: 1727377876393,
            name: 'image 2.jpg',
            sha256: '132597a99d807d12d0b13d9bf3149c6644d9f252e33896d95fc9fd177320da62',
            title: 'image 2.jpg',
            version: 20220201,
            width: 2688
        },
        assetVersion: '/dA/fe160e65-5cf4-4ef6-9b1d-47c5326fec30/asset/image 2.jpg',
        baseType: 'DOTASSET',
        contentType: 'dotAsset',
        creationDate: 1727377876409,
        extension: 'jpg',
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: true,
        host: 'SYSTEM_HOST',
        hostName: 'System Host',
        identifier: 'a991ddc5-39dc-4782-bc04-f4c4fa0ccff6',
        inode: 'fe160e65-5cf4-4ef6-9b1d-47c5326fec30',
        isContentlet: true,
        languageId: 1,
        live: true,
        locked: false,
        mimeType: 'image/jpeg',
        modDate: '1727377876407',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        name: 'image 2.jpg',
        owner: 'dotcms.org.1',
        ownerUserName: 'Admin User',
        path: '/content.fe160e65-5cf4-4ef6-9b1d-47c5326fec30',
        publishDate: 1727377876428,
        publishUser: 'dotcms.org.1',
        publishUserName: 'Admin User',
        size: 3878653,
        sortOrder: 0,
        stInode: 'f2d8a1c7-2b77-2081-bcf1-b5348988c08d',
        statusIcons:
            "<span class='greyDotIcon' style='opacity:.4'></span><span class='liveIcon'></span>",
        title: 'image 2.jpg',
        titleImage: 'asset',
        type: 'dotasset',
        url: '/content.fe160e65-5cf4-4ef6-9b1d-47c5326fec30',
        working: true
    }
};

export const NEW_FILE_EDITABLE_MOCK: { entity: DotCMSContentlet } = {
    entity: {
        AUTO_ASSIGN_WORKFLOW: false,
        __IS_NEW_CONTENT__: true,
        __icon__: 'Icon',
        archived: false,
        asset: '/dA/a1bb59eb-6708-4701-aeea-ab93c8831203/asset/docker-compose.yml',
        assetContentAsset: 'a1bb59eb-6708-4701-aeea-ab93c8831203/asset',
        assetMetaData: {
            contentType: 'text/plain; charset=ISO-8859-1',
            editableAsText: true,
            fileSize: 3786,
            isImage: false,
            length: 3786,
            modDate: 1727356262640,
            name: 'docker-compose.yml',
            sha256: 'd5d719c2f9fe025252e421b0344310d5839ff39ef9e4a38bafd93148591a2439',
            title: 'docker-compose.yml',
            version: 20220201
        },
        assetVersion: '/dA/eccdb89f-5aa1-4f0c-a9ec-aa97304d80d5/asset/docker-compose.yml',
        baseType: 'DOTASSET',
        contentType: 'dotAsset',
        creationDate: 1727356262667,
        extension: 'yml',
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: 'SYSTEM_HOST',
        hostName: 'System Host',
        identifier: 'a1bb59eb-6708-4701-aeea-ab93c8831203',
        inode: 'eccdb89f-5aa1-4f0c-a9ec-aa97304d80d5',
        isContentlet: true,
        languageId: 1,
        live: true,
        locked: false,
        mimeType: 'unknown',
        modDate: '1727356262665',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        name: 'docker-compose.yml',
        owner: 'dotcms.org.1',
        ownerUserName: 'Admin User',
        path: '/content.eccdb89f-5aa1-4f0c-a9ec-aa97304d80d5',
        publishDate: 1727356262706,
        publishUser: 'dotcms.org.1',
        publishUserName: 'Admin User',
        size: 3786,
        sortOrder: 0,
        stInode: 'f2d8a1c7-2b77-2081-bcf1-b5348988c08d',
        statusIcons:
            "<span class='greyDotIcon' style='opacity:.4'></span><span class='liveIcon'></span>",
        title: 'docker-compose.yml',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        type: 'dotasset',
        url: '/content.eccdb89f-5aa1-4f0c-a9ec-aa97304d80d5',
        working: true,
        content: 'my content'
    }
};

export const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'enterprise-angular.pdf',
    folder: '',
    id: 'temp_1e8021f973',
    image: false,
    length: 13909932,
    metadata: {
        contentType: 'application/pdf',
        editableAsText: false,
        fileSize: 13909932,
        isImage: false,
        length: 13909932,
        modDate: 1727375044693,
        name: 'enterprise-angular.pdf',
        sha256: '7f8bc1f6485876ca6d49be77917bd35ae3de99f9a56ff94a42df3217419b30cd',
        title: 'enterprise-angular.pdf',
        version: 20220201
    },
    mimeType: 'application/pdf',
    referenceUrl: '/dA/temp_1e8021f973/tmp/enterprise-angular.pdf',
    thumbnailUrl:
        '/contentAsset/image/temp_1e8021f973/tmp/filter/Thumbnail/thumbnail_w/250/thumbnail_h/250/enterprise-angular.pdf'
};

/**
 * Mock for existing workflow
 */
export const EXISTING_WORKFLOW_MOCK: DotCMSWorkflowStatus = {
    step: {
        creationDate: 1730740464988,
        enableEscalation: false,
        escalationAction: null,
        escalationTime: 0,
        id: 'd95caaa6-1ece-42b2-8663-fb01e804a149',
        myOrder: 1,
        name: 'QA',
        resolved: false,
        schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
    },
    scheme: {
        archived: false,
        creationDate: new Date(1730734538827),
        defaultScheme: false,
        description: '',
        entryActionId: null,
        id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
        mandatory: false,
        modDate: new Date(1730233225948),
        name: 'Blogs',
        system: false,
        variableName: 'Blogs'
    },
    task: {
        assignedTo: 'Admin User',
        belongsTo: null,
        createdBy: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
        creationDate: 1562955598469,
        description: '',
        dueDate: null,
        id: 'b6bca0fe-1b79-4071-84e9-b5c222fe4aef',
        inode: 'b6bca0fe-1b79-4071-84e9-b5c222fe4aef',
        languageId: 1,
        modDate: 1730740522231,
        new: false,
        status: 'd95caaa6-1ece-42b2-8663-fb01e804a149',
        title: '5 Snow Sports to Try This Winter',
        webasset: '0edfee78-2a75-4f3d-bf20-813aae15d4e9'
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
    }
};

/**
 * Mock for new workflow
 */
export const NEW_WORKFLOW_MOCK: DotCMSWorkflowStatus = {
    scheme: {
        archived: false,
        creationDate: new Date(1730822086263),
        defaultScheme: false,
        description: '',
        entryActionId: null,
        id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
        mandatory: false,
        modDate: new Date(1730233225948),
        name: 'Blogs',
        system: false,
        variableName: 'Blogs'
    },
    step: null,
    task: null,
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
    }
};

/**
 * Mock for input of the sidebar workflow component
 */
export const WORKFLOW_MOCKS: Record<'EXISTING' | 'NEW' | 'RESET', DotWorkflowState> = {
    EXISTING: {
        scheme: {
            archived: false,
            creationDate: new Date(1732809856947),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: new Date(1732554197546),
            name: 'Blogs',
            system: false,
            variableName: 'Blogs'
        },
        step: {
            creationDate: 1732859987790,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '5865d447-5df7-4fa8-81c8-f8f183f3d1a2',
            myOrder: 0,
            name: 'Editing',
            resolved: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
        },
        task: {
            assignedTo: 'Admin User',
            belongsTo: null,
            createdBy: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
            creationDate: 1732809812333,
            description: null,
            dueDate: null,
            id: '9cc41c12-f72d-431a-9b22-ef9f1067e6d9',
            inode: '9cc41c12-f72d-431a-9b22-ef9f1067e6d9',
            languageId: 1,
            modDate: 1732809830428,
            new: false,
            status: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
            title: '6b102831-e96e-459f-aa41-b5b451f8b8e1',
            webasset: '6b102831-e96e-459f-aa41-b5b451f8b8e1'
        },
        contentState: 'existing',
        resetAction: {
            actionInputs: [],
            assignable: false,
            commentable: false,
            condition: '',
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: true,
            hasSaveActionlet: false,
            hasUnarchiveActionlet: true,
            hasUnpublishActionlet: false,
            icon: 'workflowIcon',
            id: '2d1dc771-8fda-4b43-9e81-71d43a8c73e4',
            name: 'Reset Workflow',
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: '5865d447-5df7-4fa8-81c8-f8f183f3d1a2',
            nextStepCurrentStep: false,
            order: 0,
            owner: null,
            roleHierarchyForAssign: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            showOn: [
                'LOCKED',
                'PUBLISHED',
                'ARCHIVED',
                'UNPUBLISHED',
                'LISTING',
                'UNLOCKED',
                'EDITING',
                'NEW'
            ]
        }
    },
    NEW: {
        scheme: {
            archived: false,
            creationDate: new Date(1732809856947),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: new Date(1732554197546),
            name: 'Blogs',
            system: false,
            variableName: 'Blogs'
        },
        step: {
            creationDate: 1732859904768,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '5865d447-5df7-4fa8-81c8-f8f183f3d1a2',
            myOrder: 0,
            name: 'Editing',
            resolved: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
        },
        task: null,
        contentState: 'new',
        resetAction: null
    },
    RESET: {
        scheme: {
            archived: false,
            creationDate: new Date(1732809856947),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: new Date(1732554197546),
            name: 'Blogs',
            system: false,
            variableName: 'Blogs'
        },
        step: {
            creationDate: 1732860056894,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '5865d447-5df7-4fa8-81c8-f8f183f3d1a2',
            myOrder: 0,
            name: 'Editing',
            resolved: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
        },
        task: {
            assignedTo: 'Admin User',
            belongsTo: null,
            createdBy: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
            creationDate: 1732854838710,
            description: null,
            dueDate: null,
            id: 'f485675d-9e34-485d-9ec8-39a6e03b0272',
            inode: 'f485675d-9e34-485d-9ec8-39a6e03b0272',
            languageId: 1,
            modDate: 1732854838710,
            new: false,
            status: null,
            title: '74968ffd-7692-47d5-bd3a-44eeb5fbe551',
            webasset: '74968ffd-7692-47d5-bd3a-44eeb5fbe551'
        },
        contentState: 'reset',
        resetAction: null
    }
};

/**
 * Mock for input of the sidebar workflow component
 */
export const WORKFLOW_SELECTION_MOCK = {
    WITH_OPTIONS: {
        schemeOptions: [
            { label: 'System Workflow', value: '1' },
            { label: 'Marketing Workflow', value: '2' }
        ],
        isWorkflowSelected: false
    },
    NO_WORKFLOW: {
        schemeOptions: [],
        isWorkflowSelected: true
    }
};
