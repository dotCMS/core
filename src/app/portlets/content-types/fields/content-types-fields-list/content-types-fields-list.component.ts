
import { Component, OnInit } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import { FIELD_ICONS } from './content-types-fields-icon-map';
import { Field, FieldType } from '../';
import { FieldService, FieldDragDropService } from '../service';

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
    fieldTypes: Field[];

    constructor(
        private fieldService: FieldService,
        private fieldDragDropService: FieldDragDropService
    ) {}

    ngOnInit(): void {

        this.fieldService.loadFieldTypes()
            .subscribe(fieldTypes => {
                this.fieldTypes = fieldTypes.map(fieldType => {
                    return {
                        clazz: fieldType.clazz,
                        name: fieldType.label
                    };
                });
            });
        this.fieldDragDropService.setFieldBagOptions();
    }

    getIcon(id: string): string {
        return FIELD_ICONS[id];
    }
}
