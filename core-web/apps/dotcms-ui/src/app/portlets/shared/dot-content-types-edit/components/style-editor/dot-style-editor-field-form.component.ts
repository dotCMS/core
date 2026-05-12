import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';

import { DotMessageService } from '@dotcms/data-access';
import { StyleEditorFieldType } from '@dotcms/types/internal';
import { DotMessagePipe } from '@dotcms/ui';

import { BuilderField, BuilderOption, FIELD_TYPE_OPTIONS, toLabelIdentifier } from './models';

/** Per-option validation error messages. */
interface OptionErrors {
    label: string;
    value: string;
    key: string;
    imageURL: string;
}

/** Reactive form state for a single style editor field. */
interface FieldFormState {
    type: StyleEditorFieldType;
    label: string;
    identifier: string;
    identifierTouched: boolean;
    placeholder: string;
    inputType: 'text' | 'number';
    columns: 1 | 2;
    options: BuilderOption[];
    /** Tracks which option values have been manually edited (breaks auto-compute from label). */
    optionValueTouched: boolean[];
}

@Component({
    selector: 'dot-style-editor-field-form',
    imports: [
        FormsModule,
        InputTextModule,
        SelectModule,
        PanelModule,
        ButtonModule,
        SelectButtonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-style-editor-field-form.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotStyleEditorFieldFormComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly $field = input.required<BuilderField>({ alias: 'field' });
    readonly $isFirst = input<boolean>(false, { alias: 'isFirst' });
    readonly $isLast = input<boolean>(false, { alias: 'isLast' });
    readonly $showErrors = input<boolean>(false, { alias: 'showErrors' });
    readonly $isDuplicateIdentifier = input<boolean>(false, { alias: 'isDuplicateIdentifier' });

    readonly fieldChange = output<BuilderField>();
    readonly delete = output<void>();
    readonly moveUp = output<void>();
    readonly moveDown = output<void>();

    readonly fieldTypes = FIELD_TYPE_OPTIONS.map((opt) => ({
        value: opt.value,
        label: this.#dotMessageService.get(opt.labelKey)
    }));

    readonly $isCollapsed = signal(false);

    readonly columnOptions = [
        { label: '1', value: 1 },
        { label: '2', value: 2 }
    ] as const;

    readonly #state = signalState<FieldFormState>({
        type: 'input',
        label: '',
        identifier: '',
        identifierTouched: false,
        placeholder: '',
        inputType: 'text',
        columns: 1,
        options: [{ label: '', value: '' }],
        optionValueTouched: [false]
    });

    #lastUid = '';

    readonly $type = this.#state.type;
    readonly $label = this.#state.label;
    readonly $identifier = this.#state.identifier;
    readonly $identifierTouched = this.#state.identifierTouched;
    readonly $placeholder = this.#state.placeholder;
    readonly $columns = this.#state.columns;
    readonly $options = this.#state.options;

    // ── Validation signals ────────────────────────────────────────────────

    readonly $labelError = computed(() =>
        this.$showErrors() && !this.$label().trim()
            ? this.#dotMessageService.get('style.editor.form.builder.field.error.label.required')
            : ''
    );

    readonly $identifierError = computed(() => {
        if (!this.$showErrors()) return '';
        if (!this.$identifier().trim()) {
            return this.#dotMessageService.get(
                'style.editor.form.builder.field.error.identifier.required'
            );
        }

        if (this.$isDuplicateIdentifier()) {
            return this.#dotMessageService.get(
                'style.editor.form.builder.field.error.identifier.duplicate'
            );
        }

        return '';
    });

    readonly $optionsCountError = computed(() => {
        if (!this.$showErrors() || this.$type() === 'input') return '';

        return this.$options().length === 0
            ? this.#dotMessageService.get('style.editor.form.builder.field.error.options.required')
            : '';
    });

    readonly $optionErrors = computed((): OptionErrors[] => {
        const type = this.$type();
        const showErrors = this.$showErrors();

        return this.$options().map((opt) => ({
            label:
                showErrors && !opt.label?.trim()
                    ? this.#dotMessageService.get(
                          'style.editor.form.builder.field.error.option.label.required'
                      )
                    : '',
            value:
                showErrors && type !== 'checkboxGroup' && !opt.value?.trim()
                    ? this.#dotMessageService.get(
                          'style.editor.form.builder.field.error.option.value.required'
                      )
                    : '',
            key:
                showErrors && type === 'checkboxGroup' && !opt.key?.trim()
                    ? this.#dotMessageService.get(
                          'style.editor.form.builder.field.error.option.key.required'
                      )
                    : '',
            imageURL:
                showErrors &&
                type === 'radio' &&
                opt.imageURL !== undefined &&
                !opt.imageURL?.trim()
                    ? this.#dotMessageService.get(
                          'style.editor.form.builder.field.error.option.image.required'
                      )
                    : ''
        }));
    });

    readonly $hasErrors = computed(() => {
        if (!this.$showErrors()) return false;
        if (!this.$label().trim() || !this.$identifier().trim() || this.$isDuplicateIdentifier())
            return true;
        if (this.$type() === 'input') return false;

        const opts = this.$options();
        if (opts.length === 0) return true;

        const type = this.$type();

        return opts.some((opt) => {
            if (!opt.label?.trim()) {
                return true;
            }

            if (type === 'checkboxGroup') {
                return !opt.key?.trim();
            }

            if (type === 'radio' && opt.imageURL !== undefined && !opt.imageURL?.trim())
                return true;

            return false;
        });
    });

    readonly $panelPT = computed(() => ({
        root: {
            class: [
                'w-full',
                'rounded-[12px]',
                '!shadow-none',
                'overflow-hidden',
                'group/field',
                this.$hasErrors() ? '!border-red-300' : ''
            ]
        },
        header: {
            style: 'background-color: #fbfcfd',
            class: ['!min-h-[4.214rem]', 'border-b', '!border-gray-200']
        },
        content: {
            class: ['!bg-white']
        },
        pcToggleButton: {
            root: { class: '!hidden' }
        }
    }));

    constructor() {
        effect(() => {
            const field = this.$field();
            if (field.uid !== this.#lastUid) {
                this.#lastUid = field.uid;
                untracked(() => {
                    patchState(this.#state, {
                        type: field.type,
                        label: field.label,
                        identifier: field.identifier,
                        identifierTouched: false,
                        placeholder: field.placeholder ?? '',
                        inputType: field.inputType,
                        columns: field.columns,
                        options: [...field.options],
                        optionValueTouched: field.options.map(() => false)
                    });
                });
            }
        });
    }

    setLabel(value: string): void {
        if (!this.#state.identifierTouched()) {
            patchState(this.#state, { label: value, identifier: toLabelIdentifier(value) });
        } else {
            patchState(this.#state, { label: value });
        }

        this.#emitChange();
    }

    setIdentifier(value: string): void {
        patchState(this.#state, { identifier: value, identifierTouched: true });
        this.#emitChange();
    }

    resetIdentifier(): void {
        patchState(this.#state, {
            identifier: toLabelIdentifier(this.#state.label()),
            identifierTouched: false
        });
        this.#emitChange();
    }

    setType(value: StyleEditorFieldType): void {
        patchState(this.#state, { type: value });
        this.#emitChange();
    }

    setPlaceholder(value: string): void {
        patchState(this.#state, { placeholder: value });
        this.#emitChange();
    }

    setColumns(value: 1 | 2): void {
        patchState(this.#state, { columns: value });
        this.#emitChange();
    }

    addOption(): void {
        const isCheckbox = this.#state.type() === 'checkboxGroup';
        patchState(this.#state, ({ options, optionValueTouched }) => ({
            options: [...options, isCheckbox ? { label: '', key: '' } : { label: '', value: '' }],
            optionValueTouched: [...optionValueTouched, false]
        }));
        this.#emitChange();
    }

    removeOption(index: number): void {
        patchState(this.#state, ({ options, optionValueTouched }) => ({
            options: options.filter((_, i) => i !== index),
            optionValueTouched: optionValueTouched.filter((_, i) => i !== index)
        }));
        this.#emitChange();
    }

    updateOptionLabel(index: number, label: string): void {
        patchState(this.#state, ({ options, optionValueTouched }) => {
            const updated = [...options];
            updated[index] = optionValueTouched[index]
                ? { ...updated[index], label }
                : { ...updated[index], label, value: toLabelIdentifier(label) };

            return { options: updated };
        });
        this.#emitChange();
    }

    updateOptionValue(index: number, value: string): void {
        patchState(this.#state, ({ options, optionValueTouched }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], value };
            const updatedTouched = [...optionValueTouched];
            updatedTouched[index] = true;

            return { options: updated, optionValueTouched: updatedTouched };
        });
        this.#emitChange();
    }

    updateOptionKey(index: number, key: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], key };

            return { options: updated };
        });
        this.#emitChange();
    }

    updateOptionImageURL(index: number, imageURL: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], imageURL };

            return { options: updated };
        });
        this.#emitChange();
    }

    toggleOptionImageURL(index: number): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            const opt = updated[index];
            if (opt.imageURL !== undefined) {
                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                const { imageURL: _removed, ...rest } = opt;
                updated[index] = rest;
            } else {
                updated[index] = { ...opt, imageURL: '' };
            }

            return { options: updated };
        });
        this.#emitChange();
    }

    #emitChange(): void {
        this.fieldChange.emit({
            uid: this.$field().uid,
            type: this.$type(),
            label: this.$label(),
            identifier: this.$identifier(),
            inputType: this.#state.inputType(),
            placeholder: this.$placeholder(),
            columns: this.$columns(),
            options: this.$options()
        });
    }
}
