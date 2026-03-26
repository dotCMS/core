import { Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormField, form } from '@angular/forms/signals';

import { CheckboxModule } from 'primeng/checkbox';
import { TooltipModule } from 'primeng/tooltip';

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

/** Internal form model for the hide-label settings section. */
interface HideLabelModel {
    hideLabel: boolean;
}

/**
 * Settings section that controls the `hideLabel` field variable.
 * Renders a single checkbox that toggles whether the field label is hidden in the content editor.
 * Implements {@link FieldSettingsSection} so it can be composed into the custom-field settings panel.
 */
@Component({
    selector: 'dot-hide-label-settings',
    imports: [FormField, CheckboxModule, DotMessagePipe, TooltipModule],
    templateUrl: './dot-hide-label-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHideLabelSettingsComponent implements FieldSettingsSection {
    /** The content-type field whose variables are read and updated. */
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    readonly #fieldVariablesService = inject(DotFieldVariablesService);

    /** Reactive model bound to the signals-based form. */
    readonly $model = signal<HideLabelModel>({ hideLabel: false });

    protected readonly formTree = form(this.$model);

    /** Always `true` — this section has no validation constraints. */
    readonly $isValid = signal(true);

    /**
     * Emits `true` whenever the form transitions from pristine to dirty.
     * Skips the initial emission that occurs on component creation.
     */
    readonly valueChanges$: Observable<boolean> = toObservable(
        computed(() => this.formTree().dirty())
    ).pipe(skip(1));

    /** Reference to the persisted field variable, used to perform PUT instead of POST on save. */
    #fieldVariableRef: DotFieldVariable | null = null;

    /** Whether the form has unsaved changes. */
    get isDirty(): boolean {
        return this.formTree().dirty();
    }

    constructor() {
        // Syncs the form with $field whenever the signal changes.
        effect(() => {
            const hideLabelVar = (this.$field().fieldVariables || []).find(
                (v) => v.key === HIDE_LABEL_VARIABLE_KEY
            );
            this.#fieldVariableRef = hideLabelVar ?? null;

            this.$model.set({ hideLabel: hideLabelVar?.value === 'true' });
            this.formTree().reset();
        });
    }

    /**
     * Persists the current `hideLabel` value as a field variable.
     * Uses the existing variable reference (if any) to perform an update rather than a create.
     *
     * @param field - The content-type field to associate the variable with.
     * @returns An observable that emits the saved {@link DotFieldVariable}.
     */
    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { hideLabel } = this.$model();
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
