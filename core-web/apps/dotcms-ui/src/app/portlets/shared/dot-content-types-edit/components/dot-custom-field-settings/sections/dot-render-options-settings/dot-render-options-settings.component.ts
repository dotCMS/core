import { EMPTY, Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    inject,
    input,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { startWith, tap } from 'rxjs/operators';

import {
    CUSTOM_FIELD_OPTIONS_KEY,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import { FieldSettingsSection } from '../field-settings-section';

@Component({
    selector: 'dot-render-options-settings',
    imports: [ReactiveFormsModule, InputTextModule, ToggleSwitchModule, DotMessagePipe],
    templateUrl: './dot-render-options-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRenderOptionsSettingsComponent implements OnInit, FieldSettingsSection {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    form!: FormGroup;
    readonly isValid = signal(true);
    protected readonly $showAsModal = signal(false);

    readonly #fb = inject(FormBuilder);
    readonly #fieldVariablesService = inject(DotFieldVariablesService);
    readonly #destroyRef = inject(DestroyRef);

    #fieldVariableRef: DotFieldVariable | null = null;

    get isDirty(): boolean {
        return this.form?.dirty ?? false;
    }

    get valueChanges$(): Observable<unknown> {
        return this.form?.valueChanges ?? EMPTY;
    }

    ngOnInit(): void {
        const optionsVar = (this.$field().fieldVariables || []).find(
            (v) => v.key === CUSTOM_FIELD_OPTIONS_KEY
        );
        this.#fieldVariableRef = optionsVar ?? null;

        let options: Record<string, string> = {};

        try {
            options = optionsVar?.value ? JSON.parse(optionsVar.value) : {};
        } catch {
            // Malformed JSON in field variable — fall back to defaults
        }

        const initialShowAsModal = !!options['showAsModal'];

        this.form = this.#fb.group({
            showAsModal: [initialShowAsModal],
            customFieldWidth: [
                this.#parsePxToNumber(options.width, 398),
                [Validators.required, Validators.min(1)]
            ],
            customFieldHeight: [
                this.#parsePxToNumber(options.height, 400),
                [Validators.required, Validators.min(1)]
            ]
        });

        this.form.statusChanges
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => this.isValid.set(this.form.valid));

        this.form.controls['showAsModal'].valueChanges
            .pipe(startWith(initialShowAsModal), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => {
                this.$showAsModal.set(value);
                this.#toggleSizeControls(value);
            });
    }

    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const value = this.form.getRawValue();
        const fieldVariable: DotFieldVariable = {
            ...(this.#fieldVariableRef || {}),
            clazz: DotCMSClazzes.FIELD_VARIABLE,
            key: CUSTOM_FIELD_OPTIONS_KEY,
            value: JSON.stringify({
                showAsModal: value.showAsModal ?? false,
                width: `${value.customFieldWidth ?? 398}px`,
                height: `${value.customFieldHeight ?? 400}px`
            })
        };

        return this.#fieldVariablesService
            .save(field, fieldVariable)
            .pipe(tap((saved) => (this.#fieldVariableRef = saved)));
    }

    #toggleSizeControls(showAsModal: boolean): void {
        const opts = { emitEvent: false };
        if (showAsModal) {
            this.form.controls['customFieldWidth'].enable(opts);
            this.form.controls['customFieldHeight'].enable(opts);
        } else {
            this.form.controls['customFieldWidth'].disable(opts);
            this.form.controls['customFieldHeight'].disable(opts);
        }
    }

    #parsePxToNumber(value: string | undefined, defaultValue: number): number {
        if (value == null || value === '') return defaultValue;
        const parsed = parseInt(String(value).replace(/px$/i, '').trim(), 10);

        return Number.isNaN(parsed) ? defaultValue : parsed;
    }
}
