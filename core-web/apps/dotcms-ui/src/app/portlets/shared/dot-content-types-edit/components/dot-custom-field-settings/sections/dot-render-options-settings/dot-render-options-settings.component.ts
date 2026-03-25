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

interface RenderOptionsModel {
    showAsModal: boolean;
    customFieldWidth: number;
    customFieldHeight: number;
}

@Component({
    selector: 'dot-render-options-settings',
    imports: [FormField, InputTextModule, ToggleSwitchModule, DotMessagePipe],
    templateUrl: './dot-render-options-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRenderOptionsSettingsComponent implements OnInit, FieldSettingsSection {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    readonly defaultWidth = 398;
    readonly defaultHeight = 400;

    readonly #fieldVariablesService = inject(DotFieldVariablesService);

    readonly #model = signal<RenderOptionsModel>({
        showAsModal: false,
        customFieldWidth: this.defaultWidth,
        customFieldHeight: this.defaultHeight
    });

    protected readonly formTree = form(this.#model, (f) => {
        required(f.customFieldWidth);
        min(f.customFieldWidth, 1);
        required(f.customFieldHeight);
        min(f.customFieldHeight, 1);
        disabled(f.customFieldWidth, () => !this.#model().showAsModal);
        disabled(f.customFieldHeight, () => !this.#model().showAsModal);
    });

    readonly $isValid = computed(() => this.formTree().valid());
    protected readonly $showAsModal = computed(() => this.#model().showAsModal);

    readonly valueChanges$: Observable<unknown> = toObservable(
        computed(() => [this.formTree().dirty(), this.formTree().valid()])
    ).pipe(skip(1));

    #fieldVariableRef: DotFieldVariable | null = null;

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

        this.#model.set({
            showAsModal: !!options['showAsModal'],
            customFieldWidth: this.#parsePxToNumber(options['width'], this.defaultWidth),
            customFieldHeight: this.#parsePxToNumber(options['height'], this.defaultHeight)
        });
        this.formTree().reset();
    }

    save(field: DotCMSContentTypeField): Observable<DotFieldVariable> {
        const { showAsModal, customFieldWidth, customFieldHeight } = this.#model();
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
