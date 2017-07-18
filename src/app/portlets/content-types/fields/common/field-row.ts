import { FieldColumn } from './field-column';

export class FieldRow {

    columns: FieldColumn[];

    constructor(nColumns?: number) {
        this.columns = [];

        if (nColumns) {
            for (let i = 0; i < nColumns; i++) {
                this.columns[i] = {
                    fields: []
                };
            }
        }
    }

    addColumn(fieldColumn: FieldColumn): void {
        this.columns.push(fieldColumn);
    }
}