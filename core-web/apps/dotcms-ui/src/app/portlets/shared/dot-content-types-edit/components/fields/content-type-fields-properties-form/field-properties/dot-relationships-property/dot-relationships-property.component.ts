import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { AbstractControl, FormsModule, UntypedFormGroup } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

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
    imports: [
        RadioButtonModule,
        FormsModule,
        DotMessagePipe,
        DotNewRelationshipsComponent,
        DotEditRelationshipsComponent,
        DotFieldRequiredDirective,
        DotFieldValidationMessageComponent
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [DotRelationshipService]
})
export class DotRelationshipsPropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);

    readonly STATUS_NEW = 'NEW';
    readonly STATUS_EXISTING = 'EXISTING';

    property!: FieldProperty<{ [key: string]: unknown }>;
    group!: UntypedFormGroup;

    status = this.STATUS_NEW;

    editing = false;

    beforeValue!: DotRelationshipsPropertyValue;
    ngOnInit() {
        this.beforeValue = structuredClone(this.#control().value);
        this.editing = !!this.#control().value.velocityVar;
    }

    /**
     *Handle a change in the relationships property
     *
     * @param {DotRelationshipsPropertyValue} value
     * @memberof DotRelationshipsPropertyComponent
     */
    handleChange(value: DotRelationshipsPropertyValue): void {
        this.#control().setValue(value);
    }

    /**
     *Clean the relationships property's value
     *
     * @memberof DotRelationshipsPropertyComponent
     */
    clean(): void {
        this.#control().setValue(structuredClone(this.beforeValue));
    }

    /**
     * The control backing this property.
     *
     * Reached through `controls[...]` rather than `get(...)`: the parent form builds one control
     * per property before rendering this component, and unlike `get()` — which returns
     * `AbstractControl | null` for arbitrary paths — an `UntypedFormGroup`'s `controls` map is
     * declared non-nullable, so there is no absence to invent a fallback for.
     */
    #control(): AbstractControl {
        return this.group.controls[this.property.name];
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
