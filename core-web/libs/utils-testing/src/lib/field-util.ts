import { faker } from '@faker-js/faker';

import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn,
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
    ContentTypeTagField
} from '@dotcms/dotcms-models';

/**
 * Base field object used as foundation for creating fake content type fields
 */
export const BASE_FIELD: Omit<DotCMSContentTypeBaseField, 'dataType' | 'fieldType' | 'fieldTypeLabel' | 'clazz' > = {
    contentTypeId: faker.string.uuid(),
    fieldVariables: [],
    fixed: faker.datatype.boolean(),
    iDate: faker.date.recent().getTime(),
    id: faker.string.uuid(),
    indexed: faker.datatype.boolean(),
    listed: faker.datatype.boolean(),
    modDate: faker.date.recent().getTime(),
    name: faker.lorem.word(),
    readOnly: faker.datatype.boolean(),
    required: faker.datatype.boolean(),
    searchable: faker.datatype.boolean(),
    sortOrder: faker.number.int(),
    unique: faker.datatype.boolean(),
    variable: faker.lorem.word(),
    defaultValue: faker.lorem.word(),
    hint: faker.lorem.sentence(),
    regexCheck: faker.lorem.word(),
    forceIncludeInApi: faker.datatype.boolean()
};

/**
 * Column field definition for layout structure
 */
const COLUMN_FIELD = {
    ...BASE_FIELD,
    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
};

/**
 * Row field definition for layout structure
 */
const ROW_FIELD = {
    ...BASE_FIELD,
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
};

/**
 * Tab divider field definition for creating sections in the layout
 */
const TAB_FIELD = {
    ...BASE_FIELD,
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
};

/**
 * Column break field definition used for content type layout
 */
const COLUMN_BREAK_FIELD = {
    clazz: 'contenttype.column.break',
    name: 'Column'
};

/**
 * Creates a fake category field with customizable properties
 *
 * @param {Partial<ContentTypeCategoryField>} [overrides={}] - Optional properties to override defaults
 * @returns {ContentTypeCategoryField} A complete category field object
 */
export function createFakeCategoryField(
    overrides: Partial<ContentTypeCategoryField> = {}
): ContentTypeCategoryField {
    return {
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.CATEGORY,
        fieldTypeLabel: 'Category',
        categories: {
            categoryName: faker.lorem.word(),
            description: faker.lorem.sentence(),
            inode: faker.string.uuid(),
            key: faker.lorem.word(),
            keywords: faker.lorem.word(),
            sortOrder: faker.number.int()
        },
        values: 'Twelve|12\r\nTwenty|20\r\nThirty|30',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableWYSIWYGField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableDateField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableFileField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableImageField',
        dataType: DotCMSDataTypes.SYSTEM,
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
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
        ...BASE_FIELD,
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTagField',
        dataType: DotCMSDataTypes.SYSTEM,
        fieldType: DotCMSFieldTypes.TAG,
        fieldTypeLabel: 'Tag',
        ...overrides
    };
}

export class FieldUtil {
    /**
     * Determines if a field is new by checking if it has an ID
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is new (has no ID)
     */
    static isNewField(field: DotCMSContentTypeField): boolean {
        return !field.id;
    }

    /**
     * Determines if a field is a layout field (Row or Column)
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a layout field
     */
    static isLayoutField(field: DotCMSContentTypeField): boolean {
        return this.isRow(field) || this.isColumn(field);
    }

    /**
     * Determines if a field is a Row field
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a Row field
     */
    static isRow(field: DotCMSContentTypeField): boolean {
        return field.clazz === ROW_FIELD.clazz;
    }

    /**
     * Determines if a field is a Column field
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a Column field
     */
    static isColumn(field: DotCMSContentTypeField): boolean {
        return field.clazz === COLUMN_FIELD.clazz;
    }

    /**
     * Determines if a field is a Tab Divider field
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a Tab Divider field
     */
    static isTabDivider(field: DotCMSContentTypeField): boolean {
        return field.clazz === TAB_FIELD.clazz;
    }

    /**
     * Creates a new row field with the specified number of columns
     *
     * @param {number} nColumns - Number of columns to create in the row
     * @returns {DotCMSContentTypeLayoutRow} A complete row layout object
     */
    static createFieldRow(nColumns: number): DotCMSContentTypeLayoutRow {
        return {
            divider: createFakeRowField(),
            columns: new Array(nColumns).fill(null).map(() => FieldUtil.createFieldColumn())
        };
    }

    /**
     * Creates a new column with optional fields
     *
     * @param {DotCMSContentTypeField[]} [fields] - Optional array of fields to add to the column
     * @returns {DotCMSContentTypeLayoutColumn} A complete column layout object
     */
    static createFieldColumn(fields?: DotCMSContentTypeField[]): DotCMSContentTypeLayoutColumn {
        return {
            columnDivider: createFakeColumnField(),
            fields: fields || []
        };
    }

    /**
     * Creates a new Tab Divider field for content type layout
     *
     * @returns {DotCMSContentTypeLayoutRow} A layout row with a tab divider
     */
    static createFieldTabDivider(): DotCMSContentTypeLayoutRow {
        return {
            divider: {
                ...BASE_FIELD,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                dataType: DotCMSDataTypes.SYSTEM,
                fieldType: DotCMSFieldTypes.ROW,
                fieldTypeLabel: 'Tab Divider'
            } as DotCMSContentTypeField
        };
    }

    /**
     * Splits an array of fields by specific field types (Row or Tab dividers)
     * For example, if fields array contains [ROW_FIELD, COLUMN_FIELD, TEXT_FIELD, TAB_FIELD, ROW_FIELD, COLUMN_FIELD, TEXT_FIELD]
     * the result would be: [[ROW_FIELD, COLUMN_FIELD, TEXT_FIELD], [TAB_FIELD], [ROW_FIELD, COLUMN_FIELD, TEXT_FIELD]]
     *
     * @param {DotCMSContentTypeField[]} fields - Array of fields to split
     * @returns {DotCMSContentTypeField[][]} Array of field arrays split by divider types
     */
    static getRows(fields: DotCMSContentTypeField[]): DotCMSContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [ROW_FIELD.clazz, TAB_FIELD.clazz]);
    }

    /**
     * Splits an array of fields by Column field type
     * For example, if fields array contains [COLUMN_FIELD, TEXT_FIELD, COLUMN_FIELD, TEXT_FIELD]
     * the result would be: [[COLUMN_FIELD, TEXT_FIELD], [COLUMN_FIELD, TEXT_FIELD]]
     *
     * @param {DotCMSContentTypeField[]} fields - Array of fields to split
     * @returns {DotCMSContentTypeField[][]} Array of field arrays split by Column fields
     */
    static getColumns(fields: DotCMSContentTypeField[]): DotCMSContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [COLUMN_FIELD.clazz]);
    }

    /**
     * Splits an array of fields by the specified field classes
     *
     * @param {DotCMSContentTypeField[]} fields - Array of fields to split
     * @param {string[]} fieldClass - Array of field class names to split by
     * @returns {DotCMSContentTypeField[][]} Array of field arrays split by specified field classes
     */
    static splitFieldsBy(
        fields: DotCMSContentTypeField[],
        fieldClass: string[]
    ): DotCMSContentTypeField[][] {
        const result: DotCMSContentTypeField[][] = [];
        let currentFields: DotCMSContentTypeField[];

        fields.forEach((field: DotCMSContentTypeField) => {
            if (fieldClass.includes(field.clazz)) {
                currentFields = [];
                result.push(currentFields);
            }

            /*
                TODO: this code is for avoid error in edit mode when the content types don'thas
                ROW_FIELD and COLUMN_FIELD, this happend when the content types is saved in old UI
                but I dont know if this it's the bets fix
            */
            if (!currentFields) {
                currentFields = [];
                result.push(currentFields);
            }

            currentFields.push(field);
        });

        return result;
    }

    /**
     * Extracts all non-layout fields from a layout structure
     * Layout fields include Row, Column, and Tab fields
     *
     * @param {DotCMSContentTypeLayoutRow[]} layout - The layout structure containing fields
     * @returns {DotCMSContentTypeField[]} Array of all non-layout fields
     */
    static getFieldsWithoutLayout(layout: DotCMSContentTypeLayoutRow[]): DotCMSContentTypeField[] {
        return layout
            .map((row: DotCMSContentTypeLayoutRow) => row.columns || [])
            .filter((columns: DotCMSContentTypeLayoutColumn[]) => columns.length > 0)
            .reduce(
                (
                    accumulator: DotCMSContentTypeLayoutColumn[],
                    currentValue: DotCMSContentTypeLayoutColumn[]
                ) => accumulator.concat(currentValue),
                []
            )
            .map((fieldColumn) => fieldColumn.fields)
            .reduce(
                (accumulator: DotCMSContentTypeField[], currentValue: DotCMSContentTypeField[]) =>
                    accumulator.concat(currentValue),
                []
            );
    }

    /**
     * Extracts all Tab Divider fields from a layout structure
     *
     * @param {DotCMSContentTypeLayoutRow[]} layout - The layout structure containing fields
     * @returns {DotCMSContentTypeField[]} Array of all Tab Divider fields
     */
    static getTabDividerFields(layout: DotCMSContentTypeLayoutRow[]): DotCMSContentTypeField[] {
        return layout
            .map((row) => row.divider)
            .filter((field: DotCMSContentTypeField) => FieldUtil.isTabDivider(field));
    }

    /**
     * Determines if a field class is a column break
     *
     * @param {string} clazz - The field class to check
     * @returns {boolean} True if the field class represents a column break
     */
    static isColumnBreak(clazz: string): boolean {
        return clazz === COLUMN_BREAK_FIELD.clazz;
    }

    /**
     * Creates a new column break object
     *
     * @returns {{ clazz: string; name: string }} A column break object
     */
    static createColumnBreak(): { clazz: string; name: string } {
        return { ...COLUMN_BREAK_FIELD };
    }
}
