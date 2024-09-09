import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ContentletPayload } from '../../../shared/models';

interface FieldOption {
  name: string;
  value: string;
}

interface Field {
  name: string;
  type: 'select' | 'string';
  options?: FieldOption[];
}

@Component({
    selector: 'dot-uve-edit-style-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, InputTextModule, DropdownModule],
    templateUrl: './uve-edit-style-form.component.html',
    styleUrl: './uve-edit-style-form.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UveEditStyleFormComponent {
    contentlet = input.required<ContentletPayload>();

    fields = signal<Field[]>([
        {
          name: "Font Size",
          type: "select",
          options: [
            { name: "Small", value: "small" },
            { name: "Medium", value: "medium" },
            { name: "Large", value: "large" },
          ],
        },
        {
            name: "Color",
            type: "select",
            options: [
                { name: "Red", value: "red" },
                { name: "Blue", value: "blue" },
                { name: "Green", value: "green" },
            ]
        }
    ]);

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({});
        this.initForm();
    }

    private initForm(): void {
        this.fields().forEach(field => {
            this.form.addControl(field.name, this.fb.control(''));
        });
    }
}
