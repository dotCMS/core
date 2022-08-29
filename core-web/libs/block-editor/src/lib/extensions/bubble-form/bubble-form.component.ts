import { Component, EventEmitter, Output, Input } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

export interface DynamicControl<T> {
    value?: T;
    key?: string;
    label?: string;
    required?: boolean;
    controlType?: string;
    type?: string;
}

@Component({
    selector: 'dot-bubble-form',
    templateUrl: './bubble-form.component.html',
    styleUrls: ['./bubble-form.component.scss']
})
export class BubbleFormComponent {
    @Output() formValues = new EventEmitter();
    @Input() dynamicControls: DynamicControl<string>[] = [];
    form: FormGroup;

    constructor(private fb: FormBuilder) {}

    onSubmit() {
        this.formValues.emit({ ...this.form.value });
    }

    setFormValues(values) {
        this.form.setValue(values);
        // requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    buildForm() {
        this.form = this.fb.group({});
        this.dynamicControls.forEach((control) => {
            this.form.addControl(control.key, this.fb.control(control.value || null));
        });
    }
}
