import { Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
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
    imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
    templateUrl: './dot-hide-label-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHideLabelSettingsComponent implements OnInit, FieldSettingsSection {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    form!: FormGroup;
    readonly isValid = signal(true);

    get isDirty(): boolean {
        return this.form.dirty;
    }

    get valueChanges$(): Observable<unknown> {
        return this.form.valueChanges;
    }

    readonly #fb = inject(FormBuilder);
    readonly #fieldVariablesService = inject(DotFieldVariablesService);

    #fieldVariableRef: DotFieldVariable | null = null;

    ngOnInit(): void {
        const hideLabelVar = (this.$field().fieldVariables || []).find(
            (v) => v.key === HIDE_LABEL_VARIABLE_KEY
        );
        this.#fieldVariableRef = hideLabelVar ?? null;

        this.form = this.#fb.group({
            hideLabel: [hideLabelVar?.value === 'true']
        });
    }

    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { hideLabel } = this.form.getRawValue();
        const fieldVariable: DotFieldVariable = {
            ...(this.#fieldVariableRef || {}),
            clazz: DotCMSClazzes.FIELD_VARIABLE,
            key: HIDE_LABEL_VARIABLE_KEY,
            value: hideLabel ? 'true' : 'false'
        };

        return this.#fieldVariablesService
            .save(field, fieldVariable)
            .pipe(tap((saved) => (this.#fieldVariableRef = saved)));
    }
}
