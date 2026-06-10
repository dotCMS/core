import { faker } from '@faker-js/faker';

import type { ContentTypeFieldInput } from '../requests/contentType';

/** Mirrors DotCMSClazzes from dotcms-models — local to E2E, no libs dependency. */
const DotCMSClazzes = {
    SIMPLE_CONTENT_TYPE: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    ROW: 'com.dotcms.contenttype.model.field.ImmutableRowField',
    COLUMN: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
    TAB_DIVIDER: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
    LINE_DIVIDER: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
    COLUMN_BREAK: 'contenttype.column.break',
    BINARY: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
    BLOCK_EDITOR: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    CATEGORY: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
    CHECKBOX: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    CONSTANT: 'com.dotcms.contenttype.model.field.ImmutableConstantField',
    CUSTOM_FIELD: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
    DATE: 'com.dotcms.contenttype.model.field.ImmutableDateField',
    DATE_AND_TIME: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
    FILE: 'com.dotcms.contenttype.model.field.ImmutableFileField',
    HIDDEN: 'com.dotcms.contenttype.model.field.ImmutableHiddenField',
    IMAGE: 'com.dotcms.contenttype.model.field.ImmutableImageField',
    JSON: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    KEY_VALUE: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
    MULTI_SELECT: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    RADIO: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    RELATIONSHIP: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
    SELECT: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    HOST_FOLDER: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
    TAG: 'com.dotcms.contenttype.model.field.ImmutableTagField',
    TEXT: 'com.dotcms.contenttype.model.field.ImmutableTextField',
    TEXTAREA: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
    TIME: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
    WYSIWYG: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
} as const;

const DotCMSDataTypes = {
    SYSTEM: 'SYSTEM',
    TEXT: 'TEXT',
    LONG_TEXT: 'LONG_TEXT',
    DATE: 'DATE',
    BOOLEAN: 'BOOL',
    FLOAT: 'FLOAT',
    INTEGER: 'INTEGER'
} as const;

const DotCMSFieldTypes = {
    ROW: 'Row',
    COLUMN: 'Column',
    TAB_DIVIDER: 'Tab_divider',
    LINE_DIVIDER: 'Line_divider',
    COLUMN_BREAK: 'Column_break',
    BINARY: 'Binary',
    BLOCK_EDITOR: 'Story-Block',
    CATEGORY: 'Category',
    CHECKBOX: 'Checkbox',
    CONSTANT: 'Constant-Field',
    CUSTOM_FIELD: 'Custom-Field',
    DATE: 'Date',
    DATE_AND_TIME: 'Date-and-Time',
    FILE: 'File',
    HIDDEN: 'Hidden-Field',
    IMAGE: 'Image',
    JSON: 'JSON-Field',
    KEY_VALUE: 'Key-Value',
    MULTI_SELECT: 'Multi-Select',
    RADIO: 'Radio',
    RELATIONSHIP: 'Relationship',
    SELECT: 'Select',
    HOST_FOLDER: 'Host-Folder',
    TAG: 'Tag',
    TEXT: 'Text',
    TEXTAREA: 'Textarea',
    TIME: 'Time',
    WYSIWYG: 'WYSIWYG'
} as const;

/** Backward-compatible clazz aliases used across E2E specs. */
export const IMMUTABLE_SIMPLE_CONTENT_TYPE = DotCMSClazzes.SIMPLE_CONTENT_TYPE;
export const IMMUTABLE_TEXT_FIELD = DotCMSClazzes.TEXT;
export const IMMUTABLE_HOST_FOLDER_FIELD = DotCMSClazzes.HOST_FOLDER;
export const IMMUTABLE_RELATIONSHIP_FIELD = DotCMSClazzes.RELATIONSHIP;

type FieldOverrides = Partial<ContentTypeFieldInput>;

export function createFakeBaseField(): ContentTypeFieldInput {
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
        clazz: DotCMSClazzes.TEXT,
        defaultValue: faker.lorem.word(),
        hint: faker.lorem.sentence(),
        forceIncludeInApi: false
    };
}

export function createFakeCategoryField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeConstantField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeHiddenField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeHostFolderField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.HOST_FOLDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.HOST_FOLDER,
        fieldTypeLabel: 'Site or Folder',
        ...overrides
    };
}

export function createFakeTextField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TEXT,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.TEXT,
        fieldTypeLabel: 'Text',
        ...overrides
    };
}

export function createFakeLineDividerField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.LINE_DIVIDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.LINE_DIVIDER,
        fieldTypeLabel: 'Line Divider',
        ...overrides
    };
}

export function createFakeRelationshipField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeRowField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.ROW,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.ROW,
        fieldTypeLabel: 'Row',
        ...overrides
    };
}

export function createFakeColumnField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.COLUMN,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.COLUMN,
        fieldTypeLabel: 'Column',
        ...overrides
    };
}

export function createFakeRadioField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeCheckboxField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeTextAreaField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TEXTAREA,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.TEXTAREA,
        fieldTypeLabel: 'Text Area',
        ...overrides
    };
}

export function createFakeWYSIWYGField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.WYSIWYG,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.WYSIWYG,
        fieldTypeLabel: 'WYSIWYG',
        ...overrides
    };
}

export function createFakeDateField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.DATE,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.DATE,
        fieldTypeLabel: 'Date',
        ...overrides
    };
}

export function createFakeDateTimeField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.DATE_AND_TIME,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.DATE_AND_TIME,
        fieldTypeLabel: 'Date and Time',
        ...overrides
    };
}

export function createFakeTimeField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TIME,
        dataType: DotCMSDataTypes.DATE,
        fieldType: DotCMSFieldTypes.TIME,
        fieldTypeLabel: 'Time',
        ...overrides
    };
}

export function createFakeJSONField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.JSON,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.JSON,
        fieldTypeLabel: 'JSON',
        ...overrides
    };
}

export function createFakeFileField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.FILE,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.FILE,
        fieldTypeLabel: 'File',
        ...overrides
    };
}

export function createFakeImageField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.IMAGE,
        dataType: DotCMSDataTypes.TEXT,
        fieldType: DotCMSFieldTypes.IMAGE,
        fieldTypeLabel: 'Image',
        ...overrides
    };
}

export function createFakeSelectField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeTagField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TAG,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.TAG,
        fieldTypeLabel: 'Tag',
        ...overrides
    };
}

export function createFakeTabDividerField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.TAB_DIVIDER,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.TAB_DIVIDER,
        fieldTypeLabel: 'Tab Divider',
        ...overrides
    };
}

export function createFakeBlockEditorField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.BLOCK_EDITOR,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.BLOCK_EDITOR,
        fieldTypeLabel: 'Block Editor',
        ...overrides
    };
}

export function createFakeColumnBreakField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.COLUMN_BREAK,
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.COLUMN_BREAK,
        fieldTypeLabel: 'Column Break',
        ...overrides
    };
}

export function createFakeMultiSelectField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeBinaryField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeCustomField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
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

export function createFakeKeyValueField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakeBaseField(),
        clazz: DotCMSClazzes.KEY_VALUE,
        dataType: DotCMSDataTypes.LONG_TEXT,
        fieldType: DotCMSFieldTypes.KEY_VALUE,
        fieldTypeLabel: 'Key/Value Field',
        ...overrides
    };
}
