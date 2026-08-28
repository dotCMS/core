import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, inject, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';

import { MAX_INPUT_DESCRIPTIVE_LENGTH, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DOT_PANEL_NO_FOOTER } from '../../../shared/constants';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
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
    imports: [ButtonModule, PanelModule, FormField, InputTextModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-details.component.html'
})
export class DotExperimentsConfigureDetailsComponent {
    /**
     * Name leaf of the root form, carrying its max-length rule. Required-ness is not declared on
     * the field: the store names it on Start, which is what keeps the error off an untouched form
     * (AC28).
     */
    readonly $nameField = input.required<FieldTree<string>>({ alias: 'nameField' });

    /** Description leaf of the root form, carrying its max-length rule. */
    readonly $descriptionField = input.required<FieldTree<string>>({ alias: 'descriptionField' });

    /**
     * Whether the save gate is still closed — the draft does not exist yet.
     *
     * This card is not masked by it: it holds the Name and the Page the creation POST carries, so
     * it is the way through. What the flag decides is whether it also carries the press.
     */
    readonly $gated = input<boolean>(false, { alias: 'gated' });

    readonly #store = inject(DotExperimentsConfigureStore);
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    /** Takes the empty footer band out of the layout once the card stops offering the press. */
    protected readonly DOT_PANEL_NO_FOOTER = DOT_PANEL_NO_FOOTER;

    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;
    protected readonly maxDescriptionLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    /**
     * The name error is revealed by a Start press and cleared by typing, so a user who fixes the
     * field is not left staring at an error for something they have already corrected.
     */
    protected readonly $showNameRequiredError = computed<boolean>(() =>
        this.#store.$validationErrors().includes('name')
    );

    /**
     * Same rule the footer's button uses: nothing to write, or a write already on its way.
     * `$canSave` before creation asks for the Name and the Page, which is exactly the gate.
     */
    protected readonly $isSaveDisabled = computed<boolean>(
        () => !this.#store.$canSave() || this.#store.$isSaving()
    );

    /** The press that creates the draft. The store decides POST or PATCH from its own state. */
    protected onSaveDraft(): void {
        this.#dispatch.saveDraftRequested();
    }
}
