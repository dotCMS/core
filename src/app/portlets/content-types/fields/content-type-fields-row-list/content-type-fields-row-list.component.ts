import { Component } from '@angular/core';
import { FieldService, FieldDragDropService } from '../service';
import { FieldRow, LINE_DIVIDER } from '../';

/**
 * Fields row container
 *
 * @export
 * @class FieldTypesConFieldsRowListComponentainerComponent
 */
@Component({
    selector: 'content-type-fields-row-list',
    styles: [require('./content-type-fields-row-list.component.scss')],
    templateUrl: './content-type-fields-row-list.component.html',
})
export class ContentTypeFieldsRowListComponent {
    private rows: number[] = [4, 3, 2, 1];
    private fieldRows: FieldRow[];

    constructor(private fieldDragDropService: FieldDragDropService) {

    }

    ngOnInit(): void {
        this.fieldDragDropService.setFieldRowBagOptions();
        this.fieldRows = this.rows.map(nColumns => new FieldRow(nColumns));
    }
}