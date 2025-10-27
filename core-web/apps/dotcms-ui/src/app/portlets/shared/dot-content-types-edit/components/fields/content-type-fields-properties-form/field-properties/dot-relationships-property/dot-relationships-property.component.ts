import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, UntypedFormGroup } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMessageService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotFieldValidationMessageComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditRelationshipsComponent } from './dot-edit-relationship/dot-edit-relationships.component';
import { DotNewRelationshipsComponent } from './dot-new-relationships/dot-new-relationships.component';
import { DotRelationshipsPropertyValue } from './model/dot-relationships-property-value.model';
import { DotRelationshipService } from './services/dot-relationship.service';

import { FieldProperty } from '../field-properties.model';

/**
 *Component for relationships property field
 *
 * @export
 * @class DotRelationshipsPropertyComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-relationships-property',
    templateUrl: './dot-relationships-property.component.html',
    styleUrls: ['./dot-relationships-property.component.scss'],
    imports: [
        RadioButtonModule,
        FormsModule,
        DotMessagePipe,
        DotNewRelationshipsComponent,
        DotEditRelationshipsComponent,
        DotFieldRequiredDirective,
        DotFieldValidationMessageComponent
    ],
    providers: [DotRelationshipService]
})
export class DotRelationshipsPropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);

    readonly STATUS_NEW = 'NEW';
    readonly STATUS_EXISTING = 'EXISTING';

    property: FieldProperty<{ [key: string]: unknown }>;
    group: UntypedFormGroup;

    status = this.STATUS_NEW;

    editing: boolean;

    beforeValue: DotRelationshipsPropertyValue;
    ngOnInit() {
        this.beforeValue = structuredClone(this.group.get(this.property.name).value);
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
        this.group.get(this.property.name).setValue(structuredClone(this.beforeValue));
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
