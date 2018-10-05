import { FieldService } from '../service';
import { Component, OnInit } from '@angular/core';
import { filter, flatMap, toArray } from 'rxjs/operators';

import { ContentTypeField, FieldType } from '../';

/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-types-fields-list',
    styleUrls: ['./content-types-fields-list.component.scss'],
    templateUrl: './content-types-fields-list.component.html'
})
export class ContentTypesFieldsListComponent implements OnInit {
    fieldTypes: ContentTypeField[];

    constructor(public fieldService: FieldService) {}

    ngOnInit(): void {

        this.fieldService
            .loadFieldTypes()
            .pipe(
                flatMap((fields: FieldType[]) => fields),
                filter((field: FieldType) => field.id !== 'tab_divider'),
                toArray()
            ).subscribe((fields: FieldType[]) => this.fieldTypes = fields.map(fieldType => {
                return {
                    clazz: fieldType.clazz,
                    name: fieldType.label
                };
            }));
    }
}
