import { FieldColumn } from './field-column';
import { Field, LINE_DIVIDER, TAB_DIVIDER } from './';

export class FieldRow {

    columns: FieldColumn[];
    lineDivider: Field;

    constructor(nColumns?: number) {
        this.columns = [];

        if (nColumns) {
            for (let i = 0; i < nColumns; i++) {
                this.columns[i] = new FieldColumn();
            }
        }

        this.lineDivider = Object.assign({}, LINE_DIVIDER);
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
     * @param {Field[]} fields fields to add
     * @memberof FieldRow
     */
    addFields(fields:  Field[]): void {
        let offset = 0;

        if (fields[0] && fields[0].clazz === LINE_DIVIDER.clazz) {
            this.lineDivider = fields[0];
            offset = 1;
        }

        let fieldsSplitByTabDivider: Field[][] = this.splitFieldsByTabDiveder(fields.splice(offset));
        fieldsSplitByTabDivider.forEach(fields =>  {
            this.columns.push(new FieldColumn(fields));
        });
    }

    private splitFieldsByTabDiveder(fields: Field[]): Field[][] {
        let result: Field[][] = [];
        let currentFields: Field[];

        fields.map(field => {
            if (field.clazz === TAB_DIVIDER.clazz) {
                currentFields = [];
                result.push(currentFields);
            }
            currentFields.push(field);
        });
        return result;
    }
}