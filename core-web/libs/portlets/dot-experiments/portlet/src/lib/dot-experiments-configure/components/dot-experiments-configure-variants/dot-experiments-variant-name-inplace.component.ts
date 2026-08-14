import { Component, computed, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, maxLength, required } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe } from '@dotcms/ui';

/** Internal form model: the variant name being edited. */
interface VariantNameFormModel {
    name: string;
}

/**
 * Inplace rename control for a variant name.
 *
 * Reads as plain text until the pencil is pressed, then swaps in a single validated input with
 * Save and Cancel. Written for the Variants card rather than reusing the old screen's
 * `dot-experiments-inplace-edit-text`, which is ReactiveForms-based: this one is signal forms
 * throughout, matching the rest of the Configure screen.
 *
 * The editor closes as soon as Save is pressed instead of waiting for the rename round-trip — the
 * name shown afterwards is whatever the store holds, so a rejected rename simply reverts the row
 * rather than leaving an editor stuck open.
 */
@Component({
    selector: 'dot-experiments-variant-name-inplace',
    imports: [FormField, ButtonModule, InputTextModule, DotAutofocusDirective, DotMessagePipe],
    templateUrl: './dot-experiments-variant-name-inplace.component.html'
})
export class DotExperimentsVariantNameInplaceComponent {
    /** Name as it is currently persisted. Editing always starts from this value. */
    readonly $name = input.required<string>({ alias: 'name' });

    /** Hides the pencil, so the name reads as static text. */
    readonly $disabled = input(false, { alias: 'disabled' });

    /** Longest name the backend accepts. */
    readonly $maxLength = input(MAX_INPUT_TITLE_LENGTH, { alias: 'maxLength' });

    /** Emits the trimmed new name; never emits the unchanged one. */
    readonly $nameChanged = output<string>({ alias: 'nameChanged' });

    protected readonly $isEditing = signal(false);

    /**
     * Resets to the persisted name whenever it changes, which is what discards the draft after a
     * rename lands (or after one that failed and left the old name in place).
     */
    protected readonly $model = linkedSignal<VariantNameFormModel>(() => ({ name: this.$name() }));

    protected readonly formTree = form(this.$model, (f) => {
        required(f.name);
        maxLength(f.name, () => this.$maxLength());
    });

    /** Nothing to save while the name is invalid, blank, or unchanged. */
    protected readonly $isSaveDisabled = computed<boolean>(() => {
        const name = this.$model().name.trim();

        return this.formTree().invalid() || !name || name === this.$name();
    });

    protected startEditing(): void {
        if (this.$disabled()) {
            return;
        }

        this.$model.set({ name: this.$name() });
        this.$isEditing.set(true);
    }

    protected save(): void {
        if (this.$isSaveDisabled()) {
            return;
        }

        this.$nameChanged.emit(this.$model().name.trim());
        this.$isEditing.set(false);
    }

    protected cancel(): void {
        this.$model.set({ name: this.$name() });
        this.$isEditing.set(false);
    }
}
