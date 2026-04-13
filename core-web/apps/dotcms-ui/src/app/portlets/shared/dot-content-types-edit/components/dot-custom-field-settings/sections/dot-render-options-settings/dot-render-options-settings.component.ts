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
import { FormField, disabled, form, min, required } from '@angular/forms/signals';

import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { skip, tap } from 'rxjs/operators';

import {
    CUSTOM_FIELD_OPTIONS_KEY,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import { FieldSettingsSection } from '../field-settings-section';

/** Internal form model for the render-options settings section. */
interface RenderOptionsModel {
    /** Whether the custom field should open in a modal overlay. */
    showAsModal: boolean;
    /** Width of the modal in pixels (only active when `showAsModal` is true). */
    customFieldWidth: number;
    /** Height of the modal in pixels (only active when `showAsModal` is true). */
    customFieldHeight: number;
}

/**
 * Settings section for the "Render Options" of a Custom Field.
 * Controls whether the field renders inside a modal and, if so, its pixel dimensions.
 * Persists state as a single JSON field variable under {@link CUSTOM_FIELD_OPTIONS_KEY}.
 * Implements {@link FieldSettingsSection} to integrate with {@link DotCustomFieldSettingsComponent}.
 */
@Component({
    selector: 'dot-render-options-settings',
    imports: [FormField, InputTextModule, ToggleSwitchModule, DotMessagePipe],
    templateUrl: './dot-render-options-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRenderOptionsSettingsComponent implements OnInit, FieldSettingsSection {
    /** The content-type field whose variables are read and updated. */
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /** Fallback width (px) when no stored value is found. */
    readonly defaultWidth = 398;

    /** Fallback height (px) when no stored value is found. */
    readonly defaultHeight = 400;

    readonly #fieldVariablesService = inject(DotFieldVariablesService);

    /** Reactive model bound to the signals-based form. */
    readonly $model = signal<RenderOptionsModel>({
        showAsModal: false,
        customFieldWidth: this.defaultWidth,
        customFieldHeight: this.defaultHeight
    });

    /**
     * Signals-based form tree with validators.
     * Width and height are required, must be ≥ 1, and are disabled when `showAsModal` is false.
     */
    protected readonly formTree = form(this.$model, (f) => {
        required(f.customFieldWidth);
        min(f.customFieldWidth, 1);
        required(f.customFieldHeight);
        min(f.customFieldHeight, 1);
        disabled(f.customFieldWidth, () => !this.$model().showAsModal);
        disabled(f.customFieldHeight, () => !this.$model().showAsModal);
    });

    /** `true` when all form controls pass validation. */
    readonly $isValid = computed(() => this.formTree().valid());

    /** Derived from the model — used in the template to toggle the dimension inputs. */
    protected readonly $showAsModal = computed(() => this.$model().showAsModal);

    /**
     * Emits on every dirty/valid state change.
     * Skips the initial emission that occurs on component creation.
     */
    readonly valueChanges$ = toObservable(
        computed(() => [this.formTree().dirty(), this.formTree().valid()])
    ).pipe(skip(1));

    /** Reference to the persisted field variable, used to perform PUT instead of POST on save. */
    #fieldVariableRef: DotFieldVariable | null = null;

    /** Whether the form has unsaved changes. */
    get isDirty(): boolean {
        return this.formTree().dirty();
    }

    ngOnInit(): void {
        const optionsVar = (this.$field().fieldVariables || []).find(
            (v) => v.key === CUSTOM_FIELD_OPTIONS_KEY
        );
        this.#fieldVariableRef = optionsVar ?? null;

        let options: Record<string, unknown> = {};

        try {
            options = optionsVar?.value ? JSON.parse(optionsVar.value) : {};
        } catch {
            // Malformed JSON in field variable — fall back to defaults
        }

        this.$model.set({
            showAsModal: !!options['showAsModal'],
            customFieldWidth: this.#parsePxToNumber(options['width'], this.defaultWidth),
            customFieldHeight: this.#parsePxToNumber(options['height'], this.defaultHeight)
        });
        this.formTree().reset();
    }

    /**
     * Serialises the current model to JSON and persists it as a field variable.
     * Dimensions are stored with a `px` suffix (e.g. `"398px"`).
     *
     * @param field - The content-type field to associate the variable with.
     * @returns An observable that emits the saved {@link DotFieldVariable}.
     */
    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { showAsModal, customFieldWidth, customFieldHeight } = this.$model();
        const fieldVariable: DotFieldVariable = {
            ...(this.#fieldVariableRef || {}),
            clazz: DotCMSClazzes.FIELD_VARIABLE,
            key: CUSTOM_FIELD_OPTIONS_KEY,
            value: JSON.stringify({
                showAsModal,
                width: `${customFieldWidth ?? this.defaultWidth}px`,
                height: `${customFieldHeight ?? this.defaultHeight}px`
            })
        };

        return this.#fieldVariablesService
            .save(field, fieldVariable)
            .pipe(tap((saved) => (this.#fieldVariableRef = saved)));
    }

    /** Pixels only: `123`, `123px`, or JSON number. Rejects `%`, `vh`, etc. */
    #parsePxToNumber(value: unknown, defaultValue: number): number {
        if (value == null || value === '') return defaultValue;
        if (typeof value === 'number') {
            const n = Math.round(value);
            return Number.isFinite(value) && n >= 1 ? n : defaultValue;
        }
        const m = String(value)
            .trim()
            .match(/^(\d+(?:\.\d+)?)(px)?$/i);
        if (!m) return defaultValue;
        const n = Math.round(parseFloat(m[1]));
        return n >= 1 ? n : defaultValue;
    }
}
