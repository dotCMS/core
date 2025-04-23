import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn,
    DotCMSClazzes
} from '@dotcms/dotcms-models';

import {
    createFakeColumnBreakField,
    createFakeColumnField,
    createFakeTabDividerField,
    createFakeRowField
} from './dot-content-types.mock';

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
        return field.clazz === DotCMSClazzes.ROW;
    }

    /**
     * Determines if a field is a Column field
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a Column field
     */
    static isColumn(field: DotCMSContentTypeField): boolean {
        return field.clazz === DotCMSClazzes.COLUMN;
    }

    /**
     * Determines if a field is a Tab Divider field
     *
     * @param {DotCMSContentTypeField} field - The field to check
     * @returns {boolean} True if the field is a Tab Divider field
     */
    static isTabDivider(field: DotCMSContentTypeField): boolean {
        return field.clazz === DotCMSClazzes.TAB_DIVIDER;
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
            divider: createFakeTabDividerField()
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
        return FieldUtil.splitFieldsBy(fields, [DotCMSClazzes.ROW, DotCMSClazzes.TAB_DIVIDER]);
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
        return FieldUtil.splitFieldsBy(fields, [DotCMSClazzes.COLUMN]);
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
        return clazz === DotCMSClazzes.COLUMN_BREAK;
    }

    /**
     * Creates a new column break object
     *
     * @returns {{ clazz: string; name: string }} A column break object
     */
    static createColumnBreak(): { clazz: string; name: string } {
        return createFakeColumnBreakField();
    }
}
