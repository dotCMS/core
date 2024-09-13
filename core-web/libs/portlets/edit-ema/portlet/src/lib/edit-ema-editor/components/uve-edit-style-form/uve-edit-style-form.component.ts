import { ChangeDetectionStrategy, Component, input, signal, Output, EventEmitter, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ContentletPayload } from '../../../shared/models';
import { debounceTime } from 'rxjs/operators';
import { UVEStore } from '../../../store/dot-uve.store';

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
    store = inject(UVEStore);
    customComponents = this.store.customComponents();

    contentlet = input.required<ContentletPayload>();

    @Output() formValueChange = new EventEmitter<any>();

    fields = signal<Field[]>([]);


    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({});
        this.setupFormValueChanges();

        effect(() => {
            const contentType = this.contentlet().contentType;
            this.fields.set(this.customComponents[contentType] || []);
            this.initForm();
        }, {
            allowSignalWrites: true
        });
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
