import { FieldService } from '../service';
import { Component, OnInit } from '@angular/core';

import { filter, flatMap, toArray, map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ContentTypeField } from '../';
import { FieldType } from '@portlets/content-types/fields/shared';

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
    fieldTypes: Observable<ContentTypeField[]>;

    constructor(public fieldService: FieldService) {}

    ngOnInit(): void {
        this.fieldTypes = this.fieldService
            .loadFieldTypes()
            .pipe(
                flatMap((fields: FieldType[]) => fields),
                filter((field: FieldType) => field.id !== 'tab_divider'),
                map((field: FieldType) => {
                    return {
                        clazz: field.clazz,
                        name: field.label
                    };
                }),
                toArray()
            );
    }
}
