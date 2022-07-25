import { Component, OnInit } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { FieldProperty } from '../field-properties.model';
import { DATA_TYPE_PROPERTY_INFO } from '../../../service/data-type-property-info';

@Component({
    selector: 'dot-data-type-property',
    templateUrl: './data-type-property.component.html'
})
export class DataTypePropertyComponent implements OnInit {
    property: FieldProperty;
    group: UntypedFormGroup;
    radioInputs: object;

    ngOnInit(): void {
        this.radioInputs = DATA_TYPE_PROPERTY_INFO[this.property.field.clazz];

        /**
         * Workaround because of this bug: https://github.com/primefaces/primeng/issues/9162#issuecomment-686370453
         */
        const control = this.group.get(this.property.name);
        control.valueChanges.subscribe((value: string) => {
            control.setValue(value, {
                emitEvent: false
            });
        });
    }
}
