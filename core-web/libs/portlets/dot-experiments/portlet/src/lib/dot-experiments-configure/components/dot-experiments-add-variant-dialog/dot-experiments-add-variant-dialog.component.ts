import { Component, inject, signal } from '@angular/core';
import { FormField, form, maxLength } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import { MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe } from '@dotcms/ui';

/** Payload supplied via PrimeNG {@link DynamicDialogConfig}.data. */
export interface DotExperimentsAddVariantDialogData {
    /** Names already in use, so the generated fallback name never collides with one of them. */
    existingNames: string[];
}

/** What the dialog closes with. `undefined` means the user cancelled. */
export interface DotExperimentsAddVariantDialogResult {
    name: string;
}

/** i18n key of the fallback name, e.g. `Variant {0}`. */
const DEFAULT_VARIANT_NAME_KEY = 'experiments.configure.variants.add-dialog.default-name';

/** Internal form model: a single, optional name. */
interface AddVariantFormModel {
    name: string;
}

/**
 * Dialog that collects the name of a new variant.
 *
 * Opened with `DialogService.open(..., { data: DotExperimentsAddVariantDialogData, header, width:
 * ADD_VARIANT_DIALOG_WIDTH, closable: true, closeOnEscape: true })`. The header text (the dialog's
 * own title and close X) comes from PrimeNG's dialog chrome, so the caller must pass
 * `header: dm('experiments.configure.variants.add-dialog.header')`.
 *
 * The name is optional: submitting it blank closes with the next free `Variant {n}`, so the caller
 * always receives a usable name. Cancelling — X, ESC or the Cancel button — closes with `undefined`.
 */
@Component({
    selector: 'dot-experiments-add-variant-dialog',
    imports: [FormField, ButtonModule, InputTextModule, DotAutofocusDirective, DotMessagePipe],
    templateUrl: './dot-experiments-add-variant-dialog.component.html'
})
export class DotExperimentsAddVariantDialogComponent {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #config = inject<DynamicDialogConfig<DotExperimentsAddVariantDialogData>>(
        DynamicDialogConfig<DotExperimentsAddVariantDialogData>
    );
    readonly #dotMessageService = inject(DotMessageService);

    /** Same cap the old configuration screen enforces on a variant name. */
    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;

    protected readonly $model = signal<AddVariantFormModel>({ name: '' });

    protected readonly formTree = form(this.$model, (f) => {
        maxLength(f.name, this.maxNameLength);
    });

    /** Names already taken, lowercased once so the fallback lookup is a plain `Set` hit. */
    readonly #takenNames = new Set(
        (this.#config.data?.existingNames ?? []).map((name) => name.trim().toLowerCase())
    );

    /** Closes with the entered name, or with the generated fallback when it was left blank. */
    protected submitVariant(event: Event): void {
        event.preventDefault();

        if (this.formTree().invalid()) {
            this.formTree().markAsTouched();

            return;
        }

        const result: DotExperimentsAddVariantDialogResult = { name: this.#resolveName() };

        this.#dialogRef.close(result);
    }

    /** Closes without a result, same as the X and ESC. */
    protected cancel(): void {
        this.#dialogRef.close();
    }

    /**
     * The typed name, or the first `Variant {n}` not already in use.
     *
     * Numbering starts at `existingNames.length + 1` and walks up: deleting a middle variant would
     * otherwise regenerate a name that is still on screen.
     */
    #resolveName(): string {
        const typedName = this.$model().name.trim();

        if (typedName) {
            return typedName;
        }

        let index = this.#takenNames.size + 1;
        let candidate = this.#defaultNameFor(index);

        while (this.#takenNames.has(candidate.toLowerCase())) {
            candidate = this.#defaultNameFor(++index);
        }

        return candidate;
    }

    #defaultNameFor(index: number): string {
        return this.#dotMessageService.get(DEFAULT_VARIANT_NAME_KEY, String(index));
    }
}
