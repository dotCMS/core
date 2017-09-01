import { Component, OnInit } from '@angular/core';

import { FieldService, FieldDragDropService } from '../service';
import { FieldRow } from '../shared';

/**
 * Fields row container
 *
 * @export
 * @class FieldTypesConFieldsRowListComponentainerComponent
 */
@Component({
    selector: 'content-type-fields-row-list',
    styleUrls: ['./content-type-fields-row-list.component.scss'],
    templateUrl: './content-type-fields-row-list.component.html',
})
export class ContentTypeFieldsRowListComponent implements OnInit {
    rows: number[] = [4, 3, 2, 1];
    fieldRows: FieldRow[];

    constructor(private fieldDragDropService: FieldDragDropService) {

    }

    numberOfCols(n: number): number[] {
        return Array(n).fill('');
    }

    ngOnInit(): void {
        this.fieldDragDropService.setFieldRowBagOptions();
        this.fieldRows = this.rows.map(nColumns => new FieldRow(nColumns));
    }
}
