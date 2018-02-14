import { FieldService, FieldPropertyService } from '../service';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { FIELD_ICONS } from './content-types-fields-icon-map';
import { ContentTypeField, FieldType } from '../';

/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'content-types-fields-list',
    styleUrls: ['./content-types-fields-list.component.scss'],
    templateUrl: './content-types-fields-list.component.html'
})
export class ContentTypesFieldsListComponent implements  OnInit {
    fieldTypes: ContentTypeField[];

    constructor(public fieldService: FieldService) {

    }

    ngOnInit(): void {
        this.fieldService.loadFieldTypes()
            .subscribe(fields => this.fieldTypes = fields.map(fieldType => {
                return {
                    clazz: fieldType.clazz,
                    name: fieldType.label
                };
            }));
   }
}
