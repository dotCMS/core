import { faker } from '@faker-js/faker';

import {
    DotCMSContentTypeLayoutRow,
    ContentTypeCategoryField,
    DotCMSDataTypes,
    DotCMSFieldTypes,
    DotCMSContentTypeBaseField,
    ContentTypeHostFolderField,
    ContentTypeTextField,
    ContentTypeLineDividerField,
    ContentTypeRelationshipField,
    ContentTypeRowField,
    ContentTypeColumnField,
    ContentTypeRadioField,
    ContentTypeCheckboxField,
    ContentTypeTextAreaField,
    ContentTypeWYSIWYGField,
    ContentTypeDateField,
    ContentTypeDateTimeField,
    ContentTypeTimeField,
    ContentTypeJSONField,
    ContentTypeFileField,
    ContentTypeImageField,
    ContentTypeSelectField,
    ContentTypeTagField,
    DotCMSClazzes,
    ContentTypeTabDividerField,
    ContentTypeColumnBreakField,
    DotCMSContentType,
    ContentTypeBlockEditorField,
    ContentTypeMultiSelectField,
    ContentTypeBinaryField,
    ContentTypeCustomField,
    ContentTypeKeyValueField,
    ContentTypeConstantField,
    ContentTypeHiddenField,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';
import { EMPTY_SYSTEM_FIELD } from '@dotcms/utils';

export const dotcmsContentTypeBasicMock = {
    baseType: null,
    clazz: null,
    defaultType: false,
    description: null,
    detailPage: null,
    expireDateVar: null,
    fields: [],
    fixed: false,
    folder: null,
    host: null,
    iDate: null,
    id: null,
    layout: [],
    modDate: null,
    multilingualable: false,
    nEntries: null,
    name: null,
    owner: null,
    publishDateVar: null,
    system: false,
    urlMapPattern: null,
    variable: null,
    versionable: false,
    workflows: [],
    metadata: {}
} as unknown as DotCMSContentType;

export const dotcmsContentTypeFieldBasicMock: DotCMSContentTypeField = {
    ...EMPTY_SYSTEM_FIELD
};

export const fieldsWithBreakColumn: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...dotcmsContentTypeFieldBasicMock
        },
        columns: [
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    },
                    {
                        ...dotcmsContentTypeFieldBasicMock,
                        clazz: 'contenttype.column.break',
                        name: 'Column'
                    },
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            }
        ]
    }
];

export const fieldsBrokenWithColumns: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...dotcmsContentTypeFieldBasicMock
        },
        columns: [
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            },
            {
                columnDivider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
                },
                fields: [
                    {
                        ...dotcmsContentTypeFieldBasicMock
                    }
                ]
            }
        ]
    }
];

/*
Creates a fake content type with customizable properties
@param {Partial<DotCMSContentType>} [overrides={}] - Optional properties to override defaults
@returns {DotCMSContentType} A complete content type object
*/
export function createFakeContentType(
    overrides: Partial<DotCMSContentType> = {}
): DotCMSContentType {
    return {
        baseType: faker.lorem.word(),
        icon: faker.lorem.word(),
        clazz: faker.helpers.arrayElement(Object.values(DotCMSClazzes)),
        defaultType: false,
        contentType: faker.lorem.word(),
        description: faker.lorem.word(),
        detailPage: faker.lorem.word(),
        expireDateVar: faker.date.recent().toISOString(),
        fields: [],
        fixed: false,
        folder: faker.lorem.word(),
        host: faker.lorem.word(),
        iDate: faker.date.recent().getTime(),
        id: faker.string.uuid(),
        layout: [],
        modDate: faker.date.recent().getTime(),
        multilingualable: false,
        nEntries: faker.number.int(),
        name: faker.lorem.word(),
        owner: faker.lorem.word(),
        publishDateVar: faker.date.recent().toISOString(),
        system: false,
        urlMapPattern: faker.lorem.word(),
        variable: faker.lorem.word(),
        versionable: false,
        workflows: [],
        workflow: [],
        systemActionMappings: {},
        metadata: {},
        ...overrides
    };
}

/**
 * Base field object used as foundation for creating fake content type fields
 */
export type BaseField = Omit<
    DotCMSContentTypeBaseField,
    'dataType' | 'fieldType' | 'fieldTypeLabel' | 'clazz'
>;

export function createFakeBaseField(): BaseField {
    return {
        contentTypeId: faker.string.uuid(),
        fieldVariables: [],
        fixed: false,
        iDate: faker.date.recent().getTime(),
        id: faker.string.uuid(),
        indexed: false,
        listed: false,
        modDate: faker.date.recent().getTime(),
        name: faker.lorem.word(),
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 0,
        unique: false,
        variable: faker.lorem.word(),
        defaultValue: faker.lorem.word(),
        hint: faker.lorem.sentence(),
        forceIncludeInApi: false
    };
}

/**
 * Creates a fake category field with customizable properties
 *
 * @param {Partial<ContentTypeCategoryField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeCategoryField} A complete category field object
 */
export function createFakeCategoryField(
    overrides: Partial<ContentTypeCategoryField> = {}
): ContentTypeCategoryField {
    const categoryId = faker.string.uuid();

    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.CATEGORY,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.CATEGORY,
        fieldTypeLabel: 'Category',
        categories: {
            categoryName: faker.lorem.word(),
            description: faker.lorem.sentence(),
            inode: categoryId,
            key: faker.lorem.word(),
            keywords: faker.lorem.word(),
            sortOrder: faker.number.int()
        },
        values: categoryId,
        ...overrides
    };
}

/**
 * Creates a fake constant field with customizable properties
 *
 * @param {Partial<ContentTypeConstantField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeConstantField} A complete constant field object
 */
export function createFakeConstantField(
    overrides: Partial<ContentTypeConstantField> = {}
): ContentTypeConstantField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.CONSTANT,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.CONSTANT,
        fieldTypeLabel: 'Constant Field',
        values: '',
        ...overrides
    };
}

/**
 * Creates a fake hidden field with customizable properties
 *
 * @param {Partial<ContentTypeHiddenField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeHiddenField} A complete hidden field object
 */
export function createFakeHiddenField(
    overrides: Partial<ContentTypeHiddenField> = {}
): ContentTypeHiddenField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.HIDDEN,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.HIDDEN,
        fieldTypeLabel: 'Hidden Field',
        values: '',
        ...overrides
    };
}

/**
 * Creates a fake host folder field with customizable properties
 *
 * @param {Partial<ContentTypeHostFolderField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeHostFolderField} A complete host folder field object
 */
export function createFakeHostFolderField(
    overrides: Partial<ContentTypeHostFolderField> = {}
): ContentTypeHostFolderField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.HOST_FOLDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.HOST_FOLDER,
        fieldTypeLabel: 'Site or Folder',
        ...overrides
    };
}

/**
 * Creates a fake text field with customizable properties
 *
 * @param {Partial<ContentTypeTextField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeTextField} A complete text field object
 */
export function createFakeTextField(
    overrides: Partial<ContentTypeTextField> = {}
): ContentTypeTextField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TEXT,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.TEXT,
        fieldTypeLabel: 'Text',
        ...overrides
    };
}

/**
 * Creates a fake line divider field with customizable properties
 *
 * @param {Partial<ContentTypeLineDividerField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeLineDividerField} A complete line divider field object
 */
export function createFakeLineDividerField(
    overrides: Partial<ContentTypeLineDividerField> = {}
): ContentTypeLineDividerField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.LINE_DIVIDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.LINE_DIVIDER,
        fieldTypeLabel: 'Line Divider',
        ...overrides
    };
}

/**
 * Creates a fake relationship field with customizable properties
 *
 * @param {Partial<ContentTypeRelationshipField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeRelationshipField} A complete relationship field object
 */
export function createFakeRelationshipField(
    overrides: Partial<ContentTypeRelationshipField> = {}
): ContentTypeRelationshipField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.RELATIONSHIP,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.RELATIONSHIP,
        fieldTypeLabel: 'Relationships Field',
        name: 'Relationship Field',
        relationships: {
            cardinality: 0,
            isParentField: true,
            velocityVar: 'AllTypes'
        },
        skipRelationshipCreation: false,
        ...overrides
    };
}

/**
 * Creates a fake row field with customizable properties
 *
 * @param {Partial<ContentTypeRowField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeRowField} A complete row field object
 */
export function createFakeRowField(
    overrides: Partial<ContentTypeRowField> = {}
): ContentTypeRowField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.ROW,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.ROW,
        fieldTypeLabel: 'Row',
        ...overrides
    };
}

/**
 * Creates a fake column field with customizable properties
 *
 * @param {Partial<ContentTypeColumnField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeColumnField} A complete column field object
 */
export function createFakeColumnField(
    overrides: Partial<ContentTypeColumnField> = {}
): ContentTypeColumnField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.COLUMN,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.COLUMN,
        fieldTypeLabel: 'Column',
        ...overrides
    };
}

/**
 * Creates a fake radio field with customizable properties
 *
 * @param {Partial<ContentTypeRadioField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeRadioField} A complete radio field object
 */
export function createFakeRadioField(
    overrides: Partial<ContentTypeRadioField> = {}
): ContentTypeRadioField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.RADIO,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.RADIO,
        fieldTypeLabel: 'Radio',
        values: 'Yes|true\r\nNo|false',
        ...overrides
    };
}

/**
 * Creates a fake checkbox field with customizable properties
 *
 * @param {Partial<ContentTypeCheckboxField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeCheckboxField} A complete checkbox field object
 */
export function createFakeCheckboxField(
    overrides: Partial<ContentTypeCheckboxField> = {}
): ContentTypeCheckboxField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.CHECKBOX,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.CHECKBOX,
        fieldTypeLabel: 'Checkbox',
        values: 'Option 1|1\r\nOption 2|2\r\nOption 3|3',
        ...overrides
    };
}

/**
 * Creates a fake textarea field with customizable properties
 *
 * @param {Partial<ContentTypeTextAreaField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeTextAreaField} A complete textarea field object
 */
export function createFakeTextAreaField(
    overrides: Partial<ContentTypeTextAreaField> = {}
): ContentTypeTextAreaField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TEXTAREA,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.TEXTAREA,
        fieldTypeLabel: 'Text Area',
        ...overrides
    };
}

/**
 * Creates a fake WYSIWYG field with customizable properties
 *
 * @param {Partial<ContentTypeWYSIWYGField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeWYSIWYGField} A complete WYSIWYG field object
 */
export function createFakeWYSIWYGField(
    overrides: Partial<ContentTypeWYSIWYGField> = {}
): ContentTypeWYSIWYGField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.WYSIWYG,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.WYSIWYG,
        fieldTypeLabel: 'WYSIWYG',
        ...overrides
    };
}

/**
 * Creates a fake date field with customizable properties
 *
 * @param {Partial<ContentTypeDateField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeDateField} A complete date field object
 */
export function createFakeDateField(
    overrides: Partial<ContentTypeDateField> = {}
): ContentTypeDateField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.DATE,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.DATE,
        fieldTypeLabel: 'Date',
        ...overrides
    };
}

/**
 * Creates a fake date-time field with customizable properties
 *
 * @param {Partial<ContentTypeDateTimeField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeDateTimeField} A complete date-time field object
 */
export function createFakeDateTimeField(
    overrides: Partial<ContentTypeDateTimeField> = {}
): ContentTypeDateTimeField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.DATE_AND_TIME,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.DATE_AND_TIME,
        fieldTypeLabel: 'Date and Time',
        ...overrides
    };
}

/**
 * Creates a fake time field with customizable properties
 *
 * @param {Partial<ContentTypeTimeField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeTimeField} A complete time field object
 */
export function createFakeTimeField(
    overrides: Partial<ContentTypeTimeField> = {}
): ContentTypeTimeField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TIME,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.TIME,
        fieldTypeLabel: 'Time',
        ...overrides
    };
}

/**
 * Creates a fake JSON field with customizable properties
 *
 * @param {Partial<ContentTypeJSONField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeJSONField} A complete JSON field object
 */
export function createFakeJSONField(
    overrides: Partial<ContentTypeJSONField> = {}
): ContentTypeJSONField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.JSON,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.JSON,
        fieldTypeLabel: 'JSON',
        ...overrides
    };
}

/**
 * Creates a fake file field with customizable properties
 *
 * @param {Partial<ContentTypeFileField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeFileField} A complete file field object
 */
export function createFakeFileField(
    overrides: Partial<ContentTypeFileField> = {}
): ContentTypeFileField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.FILE,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.FILE,
        fieldTypeLabel: 'File',
        ...overrides
    };
}

/**
 * Creates a fake image field with customizable properties
 *
 * @param {Partial<ContentTypeImageField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeImageField} A complete image field object
 */
export function createFakeImageField(
    overrides: Partial<ContentTypeImageField> = {}
): ContentTypeImageField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.IMAGE,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.IMAGE,
        fieldTypeLabel: 'Image',
        ...overrides
    };
}

/**
 * Creates a fake select field with customizable properties
 *
 * @param {Partial<ContentTypeSelectField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeSelectField} A complete select field object
 */
export function createFakeSelectField(
    overrides: Partial<ContentTypeSelectField> = {}
): ContentTypeSelectField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.SELECT,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.SELECT,
        fieldTypeLabel: 'Select',
        values: 'Option A|a\r\nOption B|b\r\nOption C|c',
        ...overrides
    };
}

/**
 * Creates a fake tag field with customizable properties
 *
 * @param {Partial<ContentTypeTagField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeTagField} A complete tag field object
 */
export function createFakeTagField(
    overrides: Partial<ContentTypeTagField> = {}
): ContentTypeTagField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TAG,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.TAG,
        fieldTypeLabel: 'Tag',
        ...overrides
    };
}

/**
 * Creates a fake tab divider field with customizable properties
 *
 * @param {Partial<ContentTypeTabDividerField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeTabDividerField} A complete tab divider field object
 */
export function createFakeTabDividerField(
    overrides: Partial<ContentTypeTabDividerField> = {}
): ContentTypeTabDividerField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TAB_DIVIDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.TAB_DIVIDER,
        fieldTypeLabel: 'Tab Divider',
        ...overrides
    };
}

/**
 * Creates a fake block editor field with customizable properties
 *
 * @param {Partial<ContentTypeBlockEditorField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeBlockEditorField} A complete block editor field object
 */
export function createFakeBlockEditorField(
    overrides: Partial<ContentTypeBlockEditorField> = {}
): ContentTypeBlockEditorField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.BLOCK_EDITOR,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.BLOCK_EDITOR,
        fieldTypeLabel: 'Block Editor',
        ...overrides
    };
}

/**
 * Creates a fake column break field with customizable properties
 *
 * @param {Partial<ContentTypeColumnBreakField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeColumnBreakField} A complete column break field object
 */
export function createFakeColumnBreakField(
    overrides: Partial<ContentTypeColumnBreakField> = {}
): ContentTypeColumnBreakField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.COLUMN_BREAK,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.COLUMN_BREAK,
        fieldTypeLabel: 'Column Break',
        ...overrides
    };
}

/**
 * Creates a fake multi-select field with customizable properties
 *
 * @param {Partial<ContentTypeMultiSelectField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeMultiSelectField} A complete multi-select field object
 */
export function createFakeMultiSelectField(
    overrides: Partial<ContentTypeMultiSelectField> = {}
): ContentTypeMultiSelectField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.MULTI_SELECT,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.MULTI_SELECT,
        fieldTypeLabel: 'Multi Select',
        values: 'Option A|a\r\nOption B|b\r\nOption C|c',
        ...overrides
    };
}

/**
 * Creates a fake binary field with customizable properties
 *
 * @param {Partial<ContentTypeBinaryField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeBinaryField} A complete binary field object
 */
export function createFakeBinaryField(
    overrides: Partial<ContentTypeBinaryField> = {}
): ContentTypeBinaryField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.BINARY,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.BINARY,
        fieldTypeLabel: 'Binary Field',
        fieldVariables: [],
        ...overrides
    };
}

/**
 * Creates a fake custom field with customizable properties
 *
 * @param {Partial<ContentTypeCustomField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeCustomField} A complete custom field object
 */
export function createFakeCustomField(
    overrides: Partial<ContentTypeCustomField> = {}
): ContentTypeCustomField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.CUSTOM_FIELD,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.CUSTOM_FIELD,
        fieldTypeLabel: 'Custom Field',
        values: faker.lorem.sentence(),
        ...overrides
    };
}

/**
 * Creates a fake key/value field with customizable properties
 *
 * @param {Partial<ContentTypeKeyValueField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeKeyValueField} A complete key/value field object
 */
export function createFakeKeyValueField(
    overrides: Partial<ContentTypeKeyValueField> = {}
): ContentTypeKeyValueField {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.KEY_VALUE,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.KEY_VALUE,
        fieldTypeLabel: 'Key/Value Field',
        ...overrides
    };
}
