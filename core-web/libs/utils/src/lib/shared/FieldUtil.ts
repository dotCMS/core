import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutColumn,
    DotCMSContentTypeLayoutRow,
    DotCMSDataTypes
} from '@dotcms/dotcms-models';

export const EMPTY_FIELD: DotCMSContentTypeField = {
    contentTypeId: '',
    dataType: null,
    fieldType: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: null,
    iDate: null,
    id: null,
    indexed: null,
    listed: null,
    modDate: null,
    name: null,
    readOnly: null,
    required: null,
    searchable: null,
    sortOrder: null,
    unique: null,
    variable: null,
    clazz: null,
    defaultValue: null,
    hint: null,
    regexCheck: null,
    values: null
};

export const EMPTY_SYSTEM_FIELD: DotCMSContentTypeField = {
    ...EMPTY_FIELD,
    dataType: DotCMSDataTypes.SYSTEM
};

const COLUMN_FIELD = {
    ...EMPTY_SYSTEM_FIELD,
    clazz: DotCMSClazzes.COLUMN
};

const ROW_FIELD = {
    ...EMPTY_SYSTEM_FIELD,
    clazz: DotCMSClazzes.ROW
};

const TAB_FIELD = {
    ...EMPTY_SYSTEM_FIELD,
    clazz: DotCMSClazzes.TAB_DIVIDER
};

const COLUMN_BREAK_FIELD = {
    clazz: DotCMSClazzes.COLUMN_BREAK,
    name: 'Column'
};

export class FieldUtil {
    /**
     * Verify if the Field already exist
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isNewField(field: DotCMSContentTypeField): boolean {
        return !field.id;
    }

    /**
     * Return true if the field is a RowField or a ColumnField
     *
     * @static
     * @param {DotCMSContentTypeField} field
     * @returns
     * @memberof FieldUtil
     */
    static isLayoutField(field: DotCMSContentTypeField): boolean {
        return this.isRow(field) || this.isColumn(field);
    }

    /**
     * Verify if the Field is a row
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isRow(field: DotCMSContentTypeField): boolean {
        return field.clazz === ROW_FIELD.clazz;
    }

    /**
     * Verify if the Field is a column
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isColumn(field: DotCMSContentTypeField): boolean {
        return field.clazz === COLUMN_FIELD.clazz;
    }

    /**
     * Verify if the Field is a tab
     * @param {DotCMSContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isTabDivider(field: DotCMSContentTypeField): boolean {
        return field.clazz === TAB_FIELD.clazz;
    }

    /**
     * Create a new row
     * @static
     * @param {number} nColumns
     * @returns {DotCMSContentTypeLayoutRow}
     * @memberof FieldUtil
     */
    static createFieldRow(nColumns: number): DotCMSContentTypeLayoutRow {
        return {
            divider: { ...ROW_FIELD },
            columns: new Array(nColumns).fill(null).map(() => FieldUtil.createFieldColumn())
        };
    }

    /**
     * Create a new column
     *
     * @static
     * @param {DotCMSContentTypeField[]} [fields]
     * @returns {DotCMSContentTypeLayoutColumn}
     * @memberof FieldUtil
     */
    static createFieldColumn(fields?: DotCMSContentTypeField[]): DotCMSContentTypeLayoutColumn {
        return {
            columnDivider: { ...COLUMN_FIELD },
            fields: fields || []
        };
    }

    /**
     * Create a new TabField
     *
     * @static
     * @returns {DotCMSContentTypeLayoutRow}
     * @memberof FieldUtil
     */
    static createFieldTabDivider(): DotCMSContentTypeLayoutRow {
        return {
            divider: { ...TAB_FIELD }
        };
    }

    /**
     * Split the fields array by FieldDivider: for example if we have a field array like:
     * ROW_FIELD, COLUMN_FIELD,TEXT_FIELD,TAB_FIELD,ROW_FIELD,COLUMN_FIELD,TEXT_FIELD
     *
     * then you would get:
     * [ROW_FIELD, COLUMN_FIELD,TEXT_FIELD], [TAB_FIELD] , [ROW_FIELD, COLUMN_FIELD, TEXT_FIELD]
     *
     * @static
     * @param {DotCMSContentTypeField[]} fields
     * @returns {DotCMSContentTypeField[][]}
     * @memberof FieldUtil
     */
    static getRows(fields: DotCMSContentTypeField[]): DotCMSContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [ROW_FIELD.clazz, TAB_FIELD.clazz]);
    }

    /**
     * Split the fields array by ColumnField: for example if we have a field array like:
     * COLUMN_FIELD,TEXT_FIELD,COLUMN_FIELD,TEXT_FIELD
     *
     * then you would get:
     * [COLUMN_FIELD,TEXT_FIELD], [COLUMN_FIELD,TEXT_FIELD]
     *
     * @static
     * @param {DotCMSContentTypeField[]} fields
     * @returns {DotCMSContentTypeField[][]}
     * @memberof FieldUtil
     */
    static getColumns(fields: DotCMSContentTypeField[]): DotCMSContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [COLUMN_FIELD.clazz]);
    }

    /**
     * Split the fields array by fieldClass: for example if we have a field array like:
     * COLUMN_FIELD,TEXT_FIELD,COLUMN_FIELD,TEXT_FIELD
     *
     * and fieldClass is equal to 'com.dotcms.contenttype.model.field.ImmutableColumnField', then you would get:
     * [COLUMN_FIELD,TEXT_FIELD], [COLUMN_FIELD,TEXT_FIELD]
     *
     * @static
     * @param {DotCMSContentTypeField[]} fields
     * @param {string[]} fieldClass
     * @returns {DotCMSContentTypeField[][]}
     * @memberof FieldUtil
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
     * Get all the not layout fields from a layout, layout field could be RowField, ColumnFiled and TabField
     *
     * @static
     * @param {DotCMSContentTypeLayoutRow[]} layout
     * @returns {DotCMSContentTypeField[]}
     * @memberof FieldUtil
     */
    static getFieldsWithoutLayout(layout: DotCMSContentTypeLayoutRow[]): DotCMSContentTypeField[] {
        return layout
            .map((row: DotCMSContentTypeLayoutRow) => row.columns)
            .filter((columns: DotCMSContentTypeLayoutColumn[]) => !!columns)
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
     * Return just the TabField from a layout
     *
     * @static
     * @param {DotCMSContentTypeLayoutRow[]} layout
     * @returns {DotCMSContentTypeField[]}
     * @memberof FieldUtil
     */
    static getTabDividerFields(layout: DotCMSContentTypeLayoutRow[]): DotCMSContentTypeField[] {
        return layout
            .map((row) => row.divider)
            .filter((field: DotCMSContentTypeField) => FieldUtil.isTabDivider(field));
    }

    /**
     * Return true if the clazz is a column break field
     *
     * @static
     * @param {string} clazz
     * @returns {boolean}
     * @memberof FieldUtil
     */
    static isColumnBreak(clazz: string): boolean {
        return clazz === DotCMSClazzes.COLUMN_BREAK;
    }

    static createColumnBreak(): { clazz: string; name: string } {
        return { ...COLUMN_BREAK_FIELD };
    }
}
