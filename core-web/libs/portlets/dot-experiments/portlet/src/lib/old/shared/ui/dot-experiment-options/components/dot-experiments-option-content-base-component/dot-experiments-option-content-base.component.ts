import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, OnDestroy } from '@angular/core';
import { FormArray, FormGroup, FormGroupDirective } from '@angular/forms';

@Component({
    template: '',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsOptionContentBaseComponent implements OnDestroy {
    parametersList: unknown;
    operatorsList: unknown;

    form: FormGroup = inject(FormGroupDirective).form as FormGroup;
    destroy$: Subject<boolean> = new Subject<boolean>();

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

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Add a new condition to the conditions FormArray
     * @param {FormGroup} initialCondition
     **/
    setInitialCondition(initialCondition: FormGroup): void {
        this.conditionsFormArray.setControl(0, initialCondition, { emitEvent: true });
    }
}
