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

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { EditContentFormData } from '../../shared/interfaces/dot-edit-content-form.interface';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';
@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-form.component.html',
    styleUrls: ['./dot-edit-content-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFormComponent implements OnInit {
    @Input() formData!: EditContentFormData;
    @Output() formSubmit = new EventEmitter();

    private fb = inject(FormBuilder);
    form!: FormGroup;

    ngOnInit() {
        if (this.formData) {
            this.initilizeForm();
        }
    }

    /**
     * Initializes the form group with form controls for each field in the `formData` array.
     * @returns void
     */
    initilizeForm() {
        this.form = this.fb.group({});
        this.formData.layout.forEach(({ columns }) => {
            columns?.forEach((column) => {
                column.fields.forEach((field) => {
                    const fieldControl = this.initializeFormControl(field);
                    this.form.addControl(field.variable, fieldControl);
                });
            });
        });
    }

    /**
     * Initializes a form control for a given DotCMSContentTypeField.
     * @param field - The DotCMSContentTypeField to initialize the form control for.
     * @returns The initialized form control.
     */
    initializeFormControl(field: DotCMSContentTypeField) {
        const validators = [];
        if (field.required) validators.push(Validators.required);
        if (field.regexCheck) {
            try {
                const regex = new RegExp(field.regexCheck);
                validators.push(Validators.pattern(regex));
            } catch (e) {
                console.error('Invalid regex', e);
            }
        }

        return this.fb.control(
            {
                value: this.formData.contentlet?.[field.variable] ?? field.defaultValue,
                disabled: field.readOnly
            },
            { validators }
        );
    }

    /**
     * Saves the content of the form by emitting the form value through the `formSubmit` event.
     * @returns void
     */
    saveContenlet() {
        this.formSubmit.emit(this.form.value);
    }
}
