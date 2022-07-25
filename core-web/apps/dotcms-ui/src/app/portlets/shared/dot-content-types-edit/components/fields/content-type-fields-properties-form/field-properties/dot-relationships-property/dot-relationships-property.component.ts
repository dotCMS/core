import { Component, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { UntypedFormGroup } from '@angular/forms';
import * as _ from 'lodash';
import { FieldProperty } from '../field-properties.model';
import { DotRelationshipsPropertyValue } from './model/dot-relationships-property-value.model';

/**
 *Component for relationships property field
 *
 * @export
 * @class DotRelationshipsPropertyComponent
 * @implements {OnInit}
 */
@Component({
    providers: [],
    selector: 'dot-relationships-property',
    templateUrl: './dot-relationships-property.component.html',
    styleUrls: ['./dot-relationships-property.component.scss']
})
export class DotRelationshipsPropertyComponent implements OnInit {
    readonly STATUS_NEW = 'NEW';
    readonly STATUS_EXISTING = 'EXISTING';

    property: FieldProperty<{ [key: string]: unknown }>;
    group: UntypedFormGroup;

    status = this.STATUS_NEW;

    editing: boolean;

    beforeValue: DotRelationshipsPropertyValue;

    constructor(private dotMessageService: DotMessageService) {}
    ngOnInit() {
        this.beforeValue = _.cloneDeep(this.group.get(this.property.name).value);
        this.editing = !!this.group.get(this.property.name).value.velocityVar;
    }

    /**
     *Handle a change in the relationships property
     *
     * @param {DotRelationshipsPropertyValue} value
     * @memberof DotRelationshipsPropertyComponent
     */
    handleChange(value: DotRelationshipsPropertyValue): void {
        this.group.get(this.property.name).setValue(value);
    }

    /**
     *Clean the relationships property's value
     *
     * @memberof DotRelationshipsPropertyComponent
     */
    clean(): void {
        this.group.get(this.property.name).setValue(_.cloneDeep(this.beforeValue));
    }

    /**
     *Return the validation error message according with the component's state.
     *
     * @returns {string}
     * @memberof DotRelationshipsPropertyComponent
     */
    getValidationErrorMessage(): string {
        return this.status === this.STATUS_NEW
            ? this.dotMessageService.get(
                  'contenttypes.field.properties.relationships.new.error.required'
              )
            : this.dotMessageService.get(
                  'contenttypes.field.properties.relationships.edit.error.required'
              );
    }
}
