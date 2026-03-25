import { Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    inject,
    input,
    signal
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormField, form } from '@angular/forms/signals';

import { CheckboxModule } from 'primeng/checkbox';

import { skip, tap } from 'rxjs/operators';

import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable,
    HIDE_LABEL_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import { FieldSettingsSection } from '../field-settings-section';

interface HideLabelModel {
    hideLabel: boolean;
}

@Component({
    selector: 'dot-hide-label-settings',
    imports: [FormField, CheckboxModule, DotMessagePipe],
    templateUrl: './dot-hide-label-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHideLabelSettingsComponent implements OnInit, FieldSettingsSection {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    readonly #fieldVariablesService = inject(DotFieldVariablesService);

    readonly #model = signal<HideLabelModel>({ hideLabel: false });

    protected readonly formTree = form(this.#model);

    readonly $isValid = signal(true);

    readonly valueChanges$: Observable<unknown> = toObservable(
        computed(() => this.formTree().dirty())
    ).pipe(skip(1));

    #fieldVariableRef: DotFieldVariable | null = null;

    get isDirty(): boolean {
        return this.formTree().dirty();
    }

    ngOnInit(): void {
        const hideLabelVar = (this.$field().fieldVariables || []).find(
            (v) => v.key === HIDE_LABEL_VARIABLE_KEY
        );
        this.#fieldVariableRef = hideLabelVar ?? null;

        this.#model.set({ hideLabel: hideLabelVar?.value === 'true' });
        this.formTree().reset();
    }

    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { hideLabel } = this.#model();
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
