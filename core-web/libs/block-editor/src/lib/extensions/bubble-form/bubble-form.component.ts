import {
    Component,
    EventEmitter,
    Output,
    Input,
    ElementRef,
    ViewChildren,
    QueryList
} from '@angular/core';
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
    @ViewChildren('group') inputs: QueryList<ElementRef>;
    @Output() formValues = new EventEmitter();
    @Output() hide = new EventEmitter<boolean>();
    @Input() dynamicControls: DynamicControl<string>[] = [];
    form: FormGroup;

    constructor(private fb: FormBuilder) {}

    onSubmit() {
        this.formValues.emit({ ...this.form.value });
    }

    setFormValues(values) {
        this.form.setValue(values);
    }

    buildForm() {
        this.form = this.fb.group({});
        this.dynamicControls.forEach((control) => {
            this.form.addControl(control.key, this.fb.control(control.value || null));
        });
    }
}
