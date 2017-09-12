import { Field } from '../';

const TAB_DIVIDER = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
};

const LINE_DIVIDER = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
};

export class FieldUtil {
    /**
     * Verify if the Field already exist
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isNewField(field: Field): Boolean {
        return !field.id;
    }

    static isRowOrColumn(field: Field) {
        return this.isRow(field) || this.isColumn(field);
    }

    /**
     * Verify if the Field is a row
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isRow(field: Field): Boolean {
        return field.clazz === LINE_DIVIDER.clazz;
    }

    /**
     * Verify if the Field is a column
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isColumn(field: Field): Boolean {
        return field.clazz === TAB_DIVIDER.clazz;
    }

    static createLineDivider(): Field {
        return Object.assign({}, LINE_DIVIDER);
    }

    static createTabDivider(): Field {
        return Object.assign({}, TAB_DIVIDER);
    }

    static splitFieldsByLineDivider(fields: Field[]): Field[][] {
        return FieldUtil.splitFieldsBy(fields, LINE_DIVIDER.clazz);
    }

    static splitFieldsByTabDivider(fields: Field[]): Field[][] {
        return FieldUtil.splitFieldsBy(fields, TAB_DIVIDER.clazz);
    }

    static splitFieldsBy(fields: Field[], fieldClass: string): Field[][] {
        const result: Field[][] = [];
        let currentFields: Field[];

        fields.forEach(field => {
            if (field.clazz === fieldClass) {
                currentFields = [];
                result.push(currentFields);
            }

            // TODO: this code is for avoid error in edit mode when the content types dont has LINE_DIVIDER and TAB_DIVIDER,
            // this happend when the content types is saved in old UI
            // but I dont know if this it's the bets fix
            if (!currentFields) {
                currentFields = [];
                result.push(currentFields);
            }

            currentFields.push(field);
        });

        return result;
    }
}
