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

/** Backward-compatible clazz aliases used across E2E specs. */
export const IMMUTABLE_SIMPLE_CONTENT_TYPE = DotCMSClazzes.SIMPLE_CONTENT_TYPE;
export const IMMUTABLE_TEXT_FIELD = DotCMSClazzes.TEXT;
export const IMMUTABLE_HOST_FOLDER_FIELD = DotCMSClazzes.HOST_FOLDER;
export const IMMUTABLE_RELATIONSHIP_FIELD = DotCMSClazzes.RELATIONSHIP;

type FieldOverrides = Partial<ContentTypeFieldInput>;

function createFakePayloadBaseField(): ContentTypeFieldInput {
    return {
        clazz: DotCMSClazzes.TEXT,
        name: faker.lorem.word(),
        variable: faker.lorem.word(),
        sortOrder: 0
    };
}

export function createFakePayloadCategoryField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.CATEGORY,
        values: faker.string.uuid(),
        ...overrides
    };
}

export function createFakePayloadConstantField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.CONSTANT,
        values: '',
        ...overrides
    };
}

export function createFakePayloadHiddenField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.HIDDEN,
        values: '',
        ...overrides
    };
}

export function createFakePayloadHostFolderField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.HOST_FOLDER,
        ...overrides
    };
}

export function createFakePayloadTextField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.TEXT,
        ...overrides
    };
}

export function createFakePayloadLineDividerField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.LINE_DIVIDER,
        ...overrides
    };
}

export function createFakePayloadRelationshipField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.RELATIONSHIP,
        relationships: {
            velocityVar: 'AllTypes',
            cardinality: 0
        },
        ...overrides
    };
}

export function createFakePayloadRowField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.ROW,
        ...overrides
    };
}

export function createFakePayloadColumnField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.COLUMN,
        ...overrides
    };
}

export function createFakePayloadRadioField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.RADIO,
        values: 'Yes|true\r\nNo|false',
        ...overrides
    };
}

export function createFakePayloadCheckboxField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.CHECKBOX,
        values: 'Option 1|1\r\nOption 2|2\r\nOption 3|3',
        ...overrides
    };
}

export function createFakePayloadTextAreaField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.TEXTAREA,
        ...overrides
    };
}

export function createFakePayloadWYSIWYGField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.WYSIWYG,
        ...overrides
    };
}

export function createFakePayloadDateField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.DATE,
        ...overrides
    };
}

export function createFakePayloadDateTimeField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.DATE_AND_TIME,
        ...overrides
    };
}

export function createFakePayloadTimeField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.TIME,
        ...overrides
    };
}

export function createFakePayloadJSONField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.JSON,
        ...overrides
    };
}

export function createFakePayloadFileField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.FILE,
        ...overrides
    };
}

export function createFakePayloadImageField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.IMAGE,
        ...overrides
    };
}

export function createFakePayloadSelectField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.SELECT,
        values: 'Option A|a\r\nOption B|b\r\nOption C|c',
        ...overrides
    };
}

export function createFakePayloadTagField(overrides: FieldOverrides = {}): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.TAG,
        ...overrides
    };
}

export function createFakePayloadTabDividerField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.TAB_DIVIDER,
        ...overrides
    };
}

export function createFakePayloadBlockEditorField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.BLOCK_EDITOR,
        ...overrides
    };
}

export function createFakePayloadColumnBreakField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.COLUMN_BREAK,
        ...overrides
    };
}

export function createFakePayloadMultiSelectField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.MULTI_SELECT,
        values: 'Option A|a\r\nOption B|b\r\nOption C|c',
        ...overrides
    };
}

export function createFakePayloadBinaryField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.BINARY,
        ...overrides
    };
}

export function createFakePayloadCustomField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.CUSTOM_FIELD,
        values: faker.lorem.sentence(),
        ...overrides
    };
}

export function createFakePayloadKeyValueField(
    overrides: FieldOverrides = {}
): ContentTypeFieldInput {
    return {
        ...createFakePayloadBaseField(),
        clazz: DotCMSClazzes.KEY_VALUE,
        ...overrides
    };
}
