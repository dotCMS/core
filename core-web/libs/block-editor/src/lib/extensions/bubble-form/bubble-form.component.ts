import {
    Component,
    ElementRef,
    EventEmitter,
    Output,
    QueryList,
    ViewChildren
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { DynamicControl } from './model';

@Component({
    selector: 'dot-bubble-form',
    templateUrl: './bubble-form.component.html',
    styleUrls: ['./bubble-form.component.scss'],
    standalone: false
})
export class BubbleFormComponent {
    @ViewChildren('group') inputs: QueryList<ElementRef>;

    @Output() formValues = new EventEmitter();
    @Output() hide = new EventEmitter<boolean>();

    options: { customClass: string } = null;
    dynamicControls: DynamicControl<unknown>[] = [];
    form: FormGroup;

    constructor(private fb: FormBuilder) {}

    onSubmit() {
        this.formValues.emit({ ...this.form.value });
    }

    setFormValues(values) {
        this.form.setValue(values);
    }

    buildForm(controls: DynamicControl<unknown>[]) {
        this.dynamicControls = controls;
        this.form = this.fb.group({});
        this.dynamicControls.forEach((control) => {
            this.form.addControl(
                control.key,
                this.fb.control(control.value || null, control.required ? Validators.required : [])
            );
        });
    }

    cleanForm() {
        this.form = null;
    }
}
