import { DotContentTypeField, DotFieldDivider } from '../';
import { FieldColumn } from '../shared';

const COLUMN_FIELD = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
};

const ROW_FIELD = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
};

export const TAB_FIELD = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
};

export class FieldUtil {
    // private static NG_ID_SEQUENCER = new Date().getTime();
    /**
     * Verify if the Field already exist
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isNewField(field: DotContentTypeField): Boolean {
        return !field.id;
    }

    static isRowOrColumn(field: DotContentTypeField) {
        return this.isRow(field) || this.isColumn(field);
    }

    /**
     * Verify if the Field is a row
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isRow(field: DotContentTypeField): boolean {
        return field.clazz === ROW_FIELD.clazz;
    }

    /**
     * Verify if the Field is a column
     * @param DotContentTypeField field
     * @returns Boolean
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isColumn(field: DotContentTypeField): boolean {
        return field.clazz === COLUMN_FIELD.clazz;
    }

    /**
     * Verify if the Field is a tab
     * @param {DotContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isTabDivider(field: DotContentTypeField): boolean {
        return field.clazz === TAB_FIELD.clazz;
    }

    static createFieldRow(nColumns: number): DotFieldDivider {
        return {
            divider: {...ROW_FIELD},
            columns: new Array(nColumns).fill(null).map(() => FieldUtil.createFieldColumn())
        };
    }

    static createFieldColumn(): FieldColumn {
        return {
            columnDivider: {...COLUMN_FIELD},
            fields: []
        };
    }

    static createFieldTabDivider(): DotFieldDivider {
        return {
            divider: Object.assign({}, TAB_FIELD)
        };
    }

    static splitFieldsByRows(fields: DotContentTypeField[]): DotContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [ROW_FIELD.clazz, TAB_FIELD.clazz]);
    }

    static splitFieldsByTabDivider(fields: DotContentTypeField[]): DotContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [COLUMN_FIELD.clazz]);
    }

    static splitFieldsBy(fields: DotContentTypeField[], fieldClass: string[]): DotContentTypeField[][] {
        const result: DotContentTypeField[][] = [];
        let currentFields: DotContentTypeField[];

        fields.forEach((field: DotContentTypeField) => {
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
}
