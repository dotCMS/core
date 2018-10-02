import { ContentTypeField } from '../';

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

    private static NG_ID__PREFIX = 'ng-';
    /**
     * Verify if the Field already exist
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isNewField(field: ContentTypeField): Boolean {
        return !field.id || field.id.startsWith(FieldUtil.NG_ID__PREFIX);
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
    static isRow(field: ContentTypeField): boolean {
        return field.clazz === ROW_FIELD.clazz;
    }

    /**
     * Verify if the Field is a column
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isColumn(field: ContentTypeField): boolean {
        return field.clazz === COLUMN_FIELD.clazz;
    }

    /**
     * Verify if the Field is a tab
     * @param {ContentTypeField} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    static isTabDivider(field: ContentTypeField): boolean {
        return field.clazz === TAB_FIELD.clazz;
    }

    static createFieldRow(): ContentTypeField {
        return Object.assign({}, ROW_FIELD);
    }

    static createFieldColumn(): ContentTypeField {
        return Object.assign({}, COLUMN_FIELD);
    }

    static createFieldTabDivider(): ContentTypeField {
        return Object.assign({}, TAB_FIELD);
    }

    static splitFieldsByLineDivider(fields: ContentTypeField[]): ContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [ROW_FIELD.clazz, TAB_FIELD.clazz]);
    }

    static splitFieldsByTabDivider(fields: ContentTypeField[]): ContentTypeField[][] {
        return FieldUtil.splitFieldsBy(fields, [COLUMN_FIELD.clazz]);
    }

    static splitFieldsBy(fields: ContentTypeField[], fieldClass: string[]): ContentTypeField[][] {
        const result: ContentTypeField[][] = [];
        let currentFields: ContentTypeField[];

        fields.forEach((field) => {
            if (fieldClass.includes(field.clazz)) {
                currentFields = [];
                result.push(currentFields);
            }

            // TODO: this code is for avoid error in edit mode when the content types dont has ROW_FIELD and COLUMN_FIELD,
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

    static createNGID(): string {
        return `${FieldUtil.NG_ID__PREFIX}${new Date().getTime()}`;
    }
}
