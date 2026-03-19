import { EMPTY, Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    Signal,
    inject,
    input,
    signal
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { tap } from 'rxjs/operators';

import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable,
    HIDE_LABEL_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import { FieldSettingsSection } from '../field-settings-section';

@Component({
    selector: 'dot-hide-label-settings',
    standalone: true,
    imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
    templateUrl: './dot-hide-label-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHideLabelSettingsComponent implements OnInit, FieldSettingsSection {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    // Initialized in ngOnInit — safe to use after view is ready
    form!: FormGroup;

    // hideLabel has no validation rules — always valid
    readonly isValid: Signal<boolean> = signal(true);

    get isDirty(): boolean {
        return this.form?.dirty ?? false;
    }

    get valueChanges$(): Observable<unknown> {
        return this.form?.valueChanges ?? EMPTY;
    }

    private readonly fb = inject(FormBuilder);
    private readonly fieldVariablesService = inject(DotFieldVariablesService);

    private fieldVariableRef: DotFieldVariable | null = null;

    ngOnInit(): void {
        const hideLabelVar = (this.$field().fieldVariables || []).find(
            (v) => v.key === HIDE_LABEL_VARIABLE_KEY
        );
        this.fieldVariableRef = hideLabelVar ?? null;

        this.form = this.fb.group({
            hideLabel: [hideLabelVar?.value === 'true']
        });
    }

    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { hideLabel } = this.form.getRawValue();
        const fieldVariable: DotFieldVariable = {
            ...(this.fieldVariableRef || {}),
            clazz: DotCMSClazzes.FIELD_VARIABLE,
            key: HIDE_LABEL_VARIABLE_KEY,
            value: hideLabel ? 'true' : 'false'
        };

        return this.fieldVariablesService
            .save(field, fieldVariable)
            .pipe(tap((saved) => (this.fieldVariableRef = saved)));
    }
}
