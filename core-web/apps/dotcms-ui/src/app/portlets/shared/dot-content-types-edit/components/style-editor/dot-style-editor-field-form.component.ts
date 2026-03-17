import { Component, effect, input, output, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { patchState, signalState } from '@ngrx/signals';

import { StyleEditorFieldType } from '@dotcms/uve';

import {
    BuilderField,
    BuilderOption,
    FIELD_TYPE_OPTIONS,
    toLabelIdentifier
} from './models';

/** Reactive form state for a single style editor field. */
interface FieldFormState {
    /** The field widget type. */
    type: StyleEditorFieldType;
    /** Human-readable label shown above the field. */
    label: string;
    /** Unique CSS-friendly identifier for the field. */
    identifier: string;
    /** Whether the user has manually edited the identifier, breaking the auto-link from label. */
    identifierTouched: boolean;
    /** Placeholder text shown inside input fields. */
    placeholder: string;
    /** HTML input type for `input` fields. */
    inputType: 'text' | 'number';
    /** Number of columns for radio button layouts. */
    columns: 1 | 2;
    /** Configurable options for dropdown, radio, and checkboxGroup fields. */
    options: BuilderOption[];
}

@Component({
    selector: 'dot-style-editor-field-form',
    imports: [FormsModule, InputTextModule, SelectModule],
    templateUrl: './dot-style-editor-field-form.component.html'
})
export class DotStyleEditorFieldFormComponent {
    /** The field data passed from the parent section. */
    readonly $field = input.required<BuilderField>({ alias: 'field' });
    /** Whether this is the first field in the section (disables move-up). */
    readonly $isFirst = input<boolean>(false, { alias: 'isFirst' });
    /** Whether this is the last field in the section (disables move-down). */
    readonly $isLast = input<boolean>(false, { alias: 'isLast' });

    /** Emits the updated field value whenever any form control changes. */
    readonly fieldChange = output<BuilderField>();
    /** Emits when the user clicks the delete button for this field. */
    readonly delete = output<void>();
    /** Emits when the user clicks move-up. */
    readonly moveUp = output<void>();
    /** Emits when the user clicks move-down. */
    readonly moveDown = output<void>();

    /** Available field type options shown in the type dropdown. */
    readonly fieldTypes = FIELD_TYPE_OPTIONS;

    /** Internal reactive state for all form controls. */
    readonly #state = signalState<FieldFormState>({
        type: 'input',
        label: 'New Field',
        identifier: 'newField',
        identifierTouched: false,
        placeholder: '',
        inputType: 'text',
        columns: 1,
        options: [{ label: '', value: '' }]
    });

    /** UID of the last rendered field — used to detect when a different field is shown. */
    #lastUid = '';

    // Expose individual state signals for template binding
    /** Current field type signal. */
    readonly $type = this.#state.type;
    /** Current label signal. */
    readonly $label = this.#state.label;
    /** Current identifier signal. */
    readonly $identifier = this.#state.identifier;
    /** Whether the identifier has been manually edited. */
    readonly $identifierTouched = this.#state.identifierTouched;
    /** Current placeholder signal. */
    readonly $placeholder = this.#state.placeholder;
    /** Current columns signal. */
    readonly $columns = this.#state.columns;
    /** Current options signal. */
    readonly $options = this.#state.options;

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
                        options: [...field.options]
                    });
                });
            }
        });
    }

    /**
     * Updates the label and, if the identifier has not been manually touched,
     * auto-derives the identifier from the new label value. Emits the change.
     *
     * @param value - The new label string entered by the user.
     */
    setLabel(value: string): void {
        if (!this.#state.identifierTouched()) {
            patchState(this.#state, { label: value, identifier: toLabelIdentifier(value) });
        } else {
            patchState(this.#state, { label: value });
        }

        this.#emitChange();
    }

    /**
     * Updates the identifier and marks it as manually touched so that
     * further label changes no longer auto-update it. Emits the change.
     *
     * @param value - The new identifier string entered by the user.
     */
    setIdentifier(value: string): void {
        patchState(this.#state, { identifier: value, identifierTouched: true });
        this.#emitChange();
    }

    /**
     * Resets the identifier back to the auto-generated value derived from
     * the current label and clears the touched flag. Emits the change.
     */
    resetIdentifier(): void {
        patchState(this.#state, {
            identifier: toLabelIdentifier(this.#state.label()),
            identifierTouched: false
        });
        this.#emitChange();
    }

    /**
     * Updates the field type. Emits the change.
     *
     * @param value - The new `StyleEditorFieldType` value.
     */
    setType(value: StyleEditorFieldType): void {
        patchState(this.#state, { type: value });
        this.#emitChange();
    }

    /**
     * Updates the placeholder text for `input`-type fields. Emits the change.
     *
     * @param value - The new placeholder string.
     */
    setPlaceholder(value: string): void {
        patchState(this.#state, { placeholder: value });
        this.#emitChange();
    }

    /**
     * Updates the number of columns for `radio`-type fields. Emits the change.
     *
     * @param value - Either `1` or `2` columns.
     */
    setColumns(value: 1 | 2): void {
        patchState(this.#state, { columns: value });
        this.#emitChange();
    }

    /**
     * Appends a new blank option row to the options list.
     * Creates a `{ label, key }` shape for checkboxGroup fields and
     * a `{ label, value }` shape for all other option-based types.
     * Emits the change.
     */
    addOption(): void {
        const isCheckbox = this.#state.type() === 'checkboxGroup';
        patchState(this.#state, ({ options }) => ({
            options: [...options, isCheckbox ? { label: '', key: '' } : { label: '', value: '' }]
        }));
        this.#emitChange();
    }

    /**
     * Removes the option at the given index. Emits the change.
     *
     * @param index - Zero-based position of the option to remove.
     */
    removeOption(index: number): void {
        patchState(this.#state, ({ options }) => ({
            options: options.filter((_, i) => i !== index)
        }));
        this.#emitChange();
    }

    /**
     * Updates the label of the option at the given index. Emits the change.
     *
     * @param index - Zero-based position of the option.
     * @param label - New label string for the option.
     */
    updateOptionLabel(index: number, label: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], label };

            return { options: updated };
        });
        this.#emitChange();
    }

    /**
     * Updates the value of the option at the given index. Emits the change.
     *
     * @param index - Zero-based position of the option.
     * @param value - New value string for the option.
     */
    updateOptionValue(index: number, value: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], value };

            return { options: updated };
        });
        this.#emitChange();
    }

    /**
     * Updates the `key` of the option at the given index (checkboxGroup only).
     * The key is the identifier used to retrieve the boolean checked/unchecked value
     * at runtime. Emits the change.
     *
     * @param index - Zero-based position of the option.
     * @param key - New key string for the option.
     */
    updateOptionKey(index: number, key: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], key };

            return { options: updated };
        });
        this.#emitChange();
    }

    /**
     * Updates the imageURL of the option at the given index. Emits the change.
     *
     * @param index - Zero-based position of the option.
     * @param imageURL - New image URL string for the option.
     */
    updateOptionImageURL(index: number, imageURL: string): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            updated[index] = { ...updated[index], imageURL };

            return { options: updated };
        });
        this.#emitChange();
    }

    /**
     * Toggles the presence of the `imageURL` property on the option at the
     * given index — adds an empty string when absent, removes the key when present.
     * Emits the change.
     *
     * @param index - Zero-based position of the option.
     */
    toggleOptionImageURL(index: number): void {
        patchState(this.#state, ({ options }) => {
            const updated = [...options];
            const opt = updated[index];
            if (opt.imageURL !== undefined) {
                const { imageURL: _removed, ...rest } = opt;
                updated[index] = rest;
            } else {
                updated[index] = { ...opt, imageURL: '' };
            }

            return { options: updated };
        });
        this.#emitChange();
    }

    /**
     * Constructs and emits a `BuilderField` from the current form state to
     * notify the parent section that the field has been updated.
     */
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
