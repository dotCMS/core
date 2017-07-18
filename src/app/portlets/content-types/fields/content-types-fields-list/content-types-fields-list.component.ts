import { Component } from '@angular/core';
import { FieldService, FieldDragDropService } from '../service';
import { Field, FieldType } from '../';

/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'content-types-fields-list',
    styles: [require('./content-types-fields-list.component.scss')],
    templateUrl: './content-types-fields-list.component.html',
})
export class ContentTypesFieldsListComponent {
    private fieldTypes: Field[];

    constructor(private fieldService: FieldService, private fieldDragDropService: FieldDragDropService) {

    }

    ngOnInit(): void {
        this.fieldService.loadFieldTypes()
            .subscribe(fields => this.fieldTypes = fields.map(fieldType =>   {
                return {
                    dataType: fieldType.name,
                    fixed: false,
                    indexed: false,
                    name: fieldType.name,
                    required: false,
                    velocityVarName: ''
                };
            }));

        this.fieldDragDropService.setFieldBagOptions();
    }
}