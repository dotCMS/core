import { Component, computed, inject, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';

import { InputTextModule } from 'primeng/inputtext';

import { MAX_INPUT_DESCRIPTIVE_LENGTH, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/**
 * Details card of the Configure screen: the experiment's name and description.
 *
 * Both fields are leaves of the shell's root form, handed over as field trees: the card renders and
 * validates them, while the shell owns the model they write into, the rules over it and the one
 * autosave that carries them to the server. There is no save button — every change is dispatched by
 * the shell, which debounces it into a PATCH (or, before the experiment exists, feeds the creation
 * POST).
 *
 * Nothing is validated until Start/Schedule is pressed (AC28): the required error under the name
 * appears only once the store has published a `name` validation failure, and disappears again as
 * soon as the field is filled in. The length error is different — it reports the form's own rule,
 * which is live because it is about what was just typed.
 *
 * The description carries `pInputText` rather than `pTextarea`: the textarea directive subscribes
 * to `NgControl.valueChanges`, which the `NgControl` signal forms provides does not expose, so
 * pairing the two throws on init. `pInputText` applies to textareas as well, injects the same
 * themed box styling at runtime (bare `p-textarea` classes stay unstyled — PrimeNG only loads a
 * component's CSS when its directive instantiates), and is already proven next to `[formField]`
 * on the name input.
 */
@Component({
    selector: 'dot-experiments-configure-details',
    imports: [FormField, InputTextModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-details.component.html'
})
export class DotExperimentsConfigureDetailsComponent {
    /** Name leaf of the root form, carrying its required and max-length rules. */
    readonly $nameField = input.required<FieldTree<string>>({ alias: 'nameField' });

    /** Description leaf of the root form, carrying its max-length rule. */
    readonly $descriptionField = input.required<FieldTree<string>>({ alias: 'descriptionField' });

    readonly #store = inject(DotExperimentsConfigureStore);

    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;
    protected readonly maxDescriptionLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    /**
     * The name error is revealed by a Start press and cleared by typing, so a user who fixes the
     * field is not left staring at an error for something they have already corrected.
     */
    protected readonly $showNameRequiredError = computed<boolean>(
        () => this.#store.validationErrors().includes('name') && !this.$nameField()().value().trim()
    );

    /** Read off the field rather than re-measured here: the rule belongs to the form. */
    protected readonly $showNameMaxLengthError = computed<boolean>(() =>
        this.$nameField()()
            .errors()
            .some(({ kind }) => kind === 'maxLength')
    );
}
