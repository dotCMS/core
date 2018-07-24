import { FieldUtil } from '../util/field-util';
import { FieldColumn } from './field-column.model';
import { ContentTypeField } from './field.model';

export class FieldRow {
    columns: FieldColumn[];
    lineDivider: ContentTypeField;

    constructor(nColumns?: number) {
        this.columns = [];
        this.lineDivider = FieldUtil.createLineDivider();

        if (nColumns) {
            for (let i = 0; i < nColumns; i++) {
                this.columns[i] = new FieldColumn();
            }
        }

    }

    /**
     * Add field to the row. This fields could cotains a LINE _DIVIDER and TAB_DIVIDER fields
     * For example if we have a array with the follow fields types:
     * fields: [
     *   {
     *       type: 'LINE_DIVIDER',
     *       id: '1'
     *   },
     *   {
     *       type: 'TEXT',
     *       id: '2'
     *   },
     *   {
     *       type: 'DATE',
     *       id: '3'
     *   },
     *   {
     *       type: 'TAB_DIVIDER',
     *       id: '4'
     *   },
     *   {
     *       type: 'TEXT',
     *       id: '5'
     *   }
     * ]
     * It will be create two FieldColumn the first one with two fields (id:2 and id:3) and the second one
     * with one field (id:5).
     * Algo the lineDivider attribute will be set to the first field(id:1)
     * @param {ContentTypeField[]} fields fields to add
     * @memberof FieldRow
     */
    addFields(fields: ContentTypeField[]): void {
        let offset = 0;

        if (fields[0] && FieldUtil.isRow(fields[0])) {
            this.lineDivider = fields[0];
            offset = 1;
        }

        const fieldsSplitByTabDivider: ContentTypeField[][] = FieldUtil.splitFieldsByTabDivider(fields.splice(offset));
        fieldsSplitByTabDivider.forEach((tabDividerFields) => {
            this.columns.push(new FieldColumn(tabDividerFields));
        });
    }

    /**
     * Create first column
     *
     * @memberof FieldRow
     */
    addFirstColumn(): void {
        this.columns[0] = new FieldColumn();
        this.columns[0].tabDivider = { 'clazz': 'com.dotcms.contenttype.model.field.ImmutableColumnField' };
    }
}
