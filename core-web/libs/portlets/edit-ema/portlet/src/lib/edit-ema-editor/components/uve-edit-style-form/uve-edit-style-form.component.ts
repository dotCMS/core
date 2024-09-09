import { ChangeDetectionStrategy, Component, input, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ContentletPayload } from '../../../shared/models';
import { debounceTime } from 'rxjs/operators';

interface FieldOption {
  name: string;
  value: string;
}

interface Field {
  name: string;
  type: 'select' | 'string';
  options?: FieldOption[];
  id: string;
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

    @Output() formValueChange = new EventEmitter<any>();

    fields = signal<Field[]>([
        {
          name: "Font Size",
          type: "select",
          id: "fontSize",
          options: [
            { name: "Small", value: "small" },
            { name: "Medium", value: "medium" },
            { name: "Large", value: "large" },
          ],
        },
        {
            name: "Color",
            type: "select",
            id: "color",
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
        this.setupFormValueChanges();
    }

    private initForm(): void {
        this.fields().forEach(field => {
            this.form.addControl(field.id, this.fb.control(''));
        });
    }

    private setupFormValueChanges(): void {
        this.form.valueChanges.pipe(
            debounceTime(0)
        ).subscribe(value => {
            this.formValueChange.emit({
                customStyles: value,
                inode: this.contentlet().inode
            });
        });
    }
}
