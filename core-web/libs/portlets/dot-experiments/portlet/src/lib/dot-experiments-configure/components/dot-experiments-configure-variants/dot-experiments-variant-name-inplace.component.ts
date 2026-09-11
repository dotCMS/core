import { Component, computed, input, linkedSignal, output, viewChild } from '@angular/core';
import { form, FormField, maxLength, required } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { Inplace, InplaceModule } from 'primeng/inplace';
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
 * Save and Cancel. `p-inplace` owns the display/edit swap, as it does on the old screen — it also
 * gives the read state its keyboard affordance, which a bare `@if` would have to reimplement. What
 * is *not* reused is the old screen's `dot-experiments-inplace-edit-text` wrapper, which is
 * ReactiveForms-based; the editor here is signal forms, matching the rest of Configure.
 *
 * The editor closes as soon as Save is pressed instead of waiting for the rename round-trip — the
 * name shown afterwards is whatever the store holds, so a rejected rename simply reverts the row
 * rather than leaving an editor stuck open.
 */
@Component({
    selector: 'dot-experiments-variant-name-inplace',
    imports: [
        FormField,
        ButtonModule,
        InplaceModule,
        InputTextModule,
        DotAutofocusDirective,
        DotMessagePipe
    ],
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

    /**
     * The swap itself is `p-inplace`'s; this is only how Save and Cancel close it.
     *
     * `protected` rather than `#`-private: Angular rejects `viewChild` on ES private fields.
     */
    protected readonly inplace = viewChild.required(Inplace);

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

    /**
     * Editing always starts from the persisted name, whichever affordance opened the editor —
     * the pencil, or the read state's own click and keyboard handling.
     */
    protected onActivate(): void {
        this.$model.set({ name: this.$name() });
    }

    protected save(): void {
        if (this.$isSaveDisabled()) {
            return;
        }

        this.$nameChanged.emit(this.$model().name.trim());
        this.inplace().deactivate();
    }

    protected cancel(): void {
        this.$model.set({ name: this.$name() });
        this.inplace().deactivate();
    }
}
