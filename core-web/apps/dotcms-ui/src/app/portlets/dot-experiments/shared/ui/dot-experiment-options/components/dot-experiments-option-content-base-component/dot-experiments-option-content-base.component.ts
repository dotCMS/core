import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormArray, FormGroup, FormGroupDirective } from '@angular/forms';

@Component({
    standalone: true,
    template: '',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsOptionContentBaseComponent {
    parametersList;
    operatorsList;

    form: FormGroup;

    constructor(private readonly formGroupDirective: FormGroupDirective) {
        this.form = this.formGroupDirective.control as FormGroup;
    }

    get primaryFormGroup() {
        return this.form.get('primary') as FormGroup;
    }

    get conditionsFormArray() {
        return this.form.get('primary.conditions') as FormArray;
    }

    /**
     * Get all Control of the FormArray with the index of the array
     * @param index
     */
    getConditionsFormArrayItemByIndex(index: number) {
        return this.conditionsFormArray.controls[index] as FormGroup;
    }
}
