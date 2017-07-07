import { Component } from '@angular/core';
import { FieldTypesService, Field, FieldType } from '../service';
import { DragulaService } from 'ng2-dragula';

/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    providers: [
        FieldTypesService
    ],
    selector: 'content-types-fields-list',
    styles: [require('./content-types-fields-list.component.scss')],
    templateUrl: './content-types-fields-list.component.html',
})
export class ContentTypesFieldsListComponent {
    private fieldTypes: Field[];

    constructor(private fieldTypesService: FieldTypesService, private dragulaService: DragulaService) {

    }

    ngOnInit(): void {
        this.fieldTypesService.loadFieldTypes()
            .subscribe( fields => this.fieldTypes = fields.map( fieldType =>   {
                return {
                    fixed: false,
                    indexed: false,
                    name: fieldType.name,
                    required: false,
                    type: fieldType.name,
                    velocityVarName: ''
                };
            }));

        this.dragulaService.setOptions('fields-bag', {
                copy: true,
                moves: (el, target, source, sibling) => {
                    return target.dataset.dragType === 'source';
                },
            });
    }
}