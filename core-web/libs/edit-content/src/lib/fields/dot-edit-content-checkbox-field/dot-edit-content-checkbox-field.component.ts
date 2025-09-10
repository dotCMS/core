import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { distinctUntilChanged } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-checkbox-field',
    imports: [CheckboxModule, ReactiveFormsModule, FormsModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    template: `
        @for (option of $options(); track option.value) {
            <p-checkbox
                [name]="$field().variable"
                [formControl]="safeControl"
                [value]="option.value"
                [label]="option.label || null"
                [inputId]="$field().variable + '-' + option.value"
                data-testid="checkbox-option" />
        }
    `,
    styleUrls: ['./dot-edit-content-checkbox-field.component.scss']
})
export class DotEditContentCheckboxFieldComponent implements OnInit {
    #controlContainer = inject(ControlContainer);
    #destroyRef = inject(DestroyRef);

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    $options = computed(() =>
        getSingleSelectableFieldOptions(this.$field().values || '', this.$field().dataType)
    );

    /**
     * FormControl that ensures values are always arrays for PrimeNG checkbox compatibility.
     */
    protected safeControl = new FormControl<string[]>([]);

    ngOnInit() {
        const originalControl = this.formControl;
        const initialValue = this.toArray(originalControl.value);

        // Update the safe control with initial value
        this.safeControl.setValue(initialValue);

        // Sync: array (safe) → string (original)
        this.safeControl.valueChanges
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((arrayValue) => {
                const stringValue = Array.isArray(arrayValue) ? arrayValue.join(',') : '';
                if (originalControl.value !== stringValue) {
                    originalControl.setValue(stringValue, { emitEvent: false });
                }
            });

        // Sync: string (original) → array (safe)
        originalControl.valueChanges
            .pipe(takeUntilDestroyed(this.#destroyRef), distinctUntilChanged())
            .subscribe((stringValue) => {
                const arrayValue = this.toArray(stringValue);
                if (JSON.stringify(this.safeControl.value) !== JSON.stringify(arrayValue)) {
                    this.safeControl.setValue(arrayValue, { emitEvent: false });
                }
            });

        // Only normalize if needed
        const normalized = initialValue.join(',');
        if (originalControl.value !== normalized) {
            originalControl.setValue(normalized, { emitEvent: false });
        }
    }

    /**
     * Converts any value to array format for checkbox handling.
     */
    private toArray(value: unknown): string[] {
        if (Array.isArray(value)) return value;
        if (value && typeof value === 'string') {
            return value
                .split(',')
                .map((v) => v.trim())
                .filter((v) => v);
        }

        return [];
    }

    /**
     * Returns the original FormControl for the checkbox field.
     */
    get formControl(): FormControl {
        return this.#controlContainer.control.get(this.$field().variable) as FormControl;
    }
}
