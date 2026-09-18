import {
    Component,
    ElementRef,
    EventEmitter,
    Output,
    QueryList,
    ViewChildren,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { BubbleFormValues, DynamicControl } from './model';

@Component({
    selector: 'dot-bubble-form',
    templateUrl: './bubble-form.component.html',
    styleUrls: ['./bubble-form.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class BubbleFormComponent {
    // Populated by Angular after the first change detection pass over the `@if (form)` block.
    @ViewChildren('group') inputs!: QueryList<ElementRef>;

    @Output() formValues = new EventEmitter<BubbleFormValues>();
    @Output() hide = new EventEmitter<boolean>();

    // Null until `buildForm` runs, and reset to null by `cleanForm`. The template gates the
    // whole form on `@if (form)`, so the nullable declaration matches what it already handles.
    options: { customClass: string } | null = null;
    dynamicControls: DynamicControl<string | boolean>[] = [];
    form: FormGroup | null = null;

    private readonly fb = inject(FormBuilder);

    onSubmit() {
        this.formValues.emit({ ...this.form?.value });
    }

    setFormValues(values: BubbleFormValues) {
        this.form?.setValue(values);
    }

    buildForm(controls: DynamicControl<string | boolean>[]) {
        this.dynamicControls = controls;
        const form = this.fb.group({});
        this.dynamicControls.forEach((control) => {
            // `key` is optional on DynamicControl; skipping is the only sane option, since
            // `addControl` was previously being handed `undefined` as a control name.
            if (!control.key) {
                return;
            }

            form.addControl(
                control.key,
                this.fb.control(control.value || null, control.required ? Validators.required : [])
            );
        });
        this.form = form;
    }

    cleanForm() {
        this.form = null;
    }
}
