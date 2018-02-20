import { ContentTypeField } from '../';

const TAB_DIVIDER = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField'
};

const LINE_DIVIDER = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
};

export class FieldUtil {
    /**
     * Verify if the Field already exist
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isNewField(field: ContentTypeField): Boolean {
        return !field.id;
    }

    static isRowOrColumn(field: ContentTypeField) {
        return this.isRow(field) || this.isColumn(field);
    }

    /**
     * Verify if the Field is a row
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isRow(field: ContentTypeField): Boolean {
        return field.clazz === LINE_DIVIDER.clazz;
    }

    /**
     * Verify if the Field is a column
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isColumn(field: ContentTypeField): Boolean {
        return field.clazz === TAB_DIVIDER.clazz;
    }

    static createLineDivider(): ContentTypeField {
        return Object.assign({}, LINE_DIVIDER);
    }

    static createTabDivider(): ContentTypeField {
        return Object.assign({}, TAB_DIVIDER);
    }

    static splitFieldsByLineDivider(fields: ContentTypeField[]): ContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, LINE_DIVIDER.clazz);
    }

    static splitFieldsByTabDivider(fields: ContentTypeField[]): ContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, TAB_DIVIDER.clazz);
    }

    static splitFieldsBy(fields: ContentTypeField[], fieldClass: string): ContentTypeField[][] {
        const result: ContentTypeField[][] = [];
        let currentFields: ContentTypeField[];

        fields.forEach((field) => {
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
