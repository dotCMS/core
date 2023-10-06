import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    inject
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotField, DotForm } from '../../interfaces/dot-form.interface';
import { DotFieldComponent } from '../dot-field/dot-field.component';
@Component({
    selector: 'dot-edit-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, DotFieldComponent, ButtonModule],
    templateUrl: './dot-form.component.html',
    styleUrls: ['./dot-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFormComponent implements OnInit {
    @Input() formData: DotForm[] = [];
    @Output() formSubmit = new EventEmitter();

    private fb = inject(FormBuilder);
    form!: FormGroup;

    ngOnInit() {
        if (this.formData) {
            this.initilizeForm();
        }
    }

    initilizeForm() {
        this.form = this.fb.group({});
        this.formData.forEach(({ row }) => {
            row.columns.forEach((column) => {
                column.fields.forEach((field) => {
                    const fieldControl = this.initializeFormControl(field);
                    this.form.addControl(field.variable, fieldControl);
                });
            });
        });
    }

    initializeFormControl(field: DotField) {
        const validators = [];
        if (field.required) validators.push(Validators.required);
        if (field.regexCheck) validators.push(Validators.pattern(field.regexCheck));

        return this.fb.control(null, { validators });
    }

    saveContenlet() {
        this.formSubmit.emit(this.form.value);
    }
}
