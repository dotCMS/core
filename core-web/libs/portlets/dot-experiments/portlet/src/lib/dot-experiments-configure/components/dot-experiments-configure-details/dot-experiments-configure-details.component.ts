import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormField, disabled, form, maxLength, required } from '@angular/forms/signals';

import { InputTextModule } from 'primeng/inputtext';

import { MAX_INPUT_DESCRIPTIVE_LENGTH, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/** Name and description of the experiment, as typed. */
interface DetailsFormModel {
    name: string;
    description: string;
}

/**
 * Details card of the Configure screen: the experiment's name and description.
 *
 * There is no save button — every change is dispatched to the store, which debounces it into a
 * PATCH (or, before the experiment exists, feeds the creation POST). The card therefore hydrates
 * its form once per loaded experiment and is the source of truth for what is on screen from then
 * on: re-reading the store on every response would overwrite what the user is typing while a
 * PATCH is in flight.
 *
 * Nothing is validated until Start/Schedule is pressed (AC28): the error under the name appears
 * only once the store has published a `name` validation failure, and disappears again as soon as
 * the field is filled in.
 *
 * Injects the store the Configure shell provides; it is not standalone-routable on its own.
 *
 * The description carries PrimeNG's textarea classes rather than its `pTextarea` directive: the
 * directive subscribes to `NgControl.valueChanges`, which the `NgControl` signal forms provides
 * does not expose, so pairing the two throws on init.
 */
@Component({
    selector: 'dot-experiments-configure-details',
    imports: [FormField, InputTextModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-details.component.html'
})
export class DotExperimentsConfigureDetailsComponent {
    readonly #store = inject(DotExperimentsConfigureStore);
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;
    protected readonly maxDescriptionLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    protected readonly $model = signal<DetailsFormModel>({ name: '', description: '' });

    protected readonly formTree = form(this.$model, (f) => {
        required(f.name);
        maxLength(f.name, this.maxNameLength);
        maxLength(f.description, this.maxDescriptionLength);
        disabled(f.name, { when: () => this.#store.$isLocked() });
        disabled(f.description, { when: () => this.#store.$isLocked() });
    });

    /**
     * The name error is revealed by a Start press and cleared by typing, so a user who fixes the
     * field is not left staring at an error for something they have already corrected.
     */
    protected readonly $showNameRequiredError = computed<boolean>(
        () => this.#store.validationErrors().includes('name') && !this.$model().name.trim()
    );

    protected readonly $showNameMaxLengthError = computed<boolean>(
        () => this.$model().name.length > this.maxNameLength
    );

    /** Identifier of the experiment whose values are already in the form. */
    readonly #hydratedExperimentId = signal<string | null>(null);

    /**
     * Fills the form from the store once per experiment.
     *
     * Keyed on the experiment's identifier rather than on the values themselves: every autosave
     * response replaces `experiment`, and re-reading it would drop characters typed while the
     * PATCH was travelling.
     */
    protected readonly hydrateEffect = effect(() => {
        const experimentId = this.#store.experiment()?.id ?? null;

        if (!experimentId || experimentId === untracked(this.#hydratedExperimentId)) {
            return;
        }

        untracked(() => {
            this.#hydratedExperimentId.set(experimentId);
            this.$model.set({
                name: this.#store.draftName(),
                description: this.#store.draftDescription()
            });
        });
    });

    /**
     * Reports what changed to the store, which owns the debounce (AC6) — the card must not add
     * one of its own.
     *
     * A blank name is deliberately never dispatched: it is rejected by the backend, and letting it
     * through would only queue a PATCH that cannot succeed. The typed value stays on screen, and
     * the experiment keeps the name it was saved with until a non-blank one replaces it.
     */
    protected readonly dispatchChangesEffect = effect(() => {
        const { name, description } = this.$model();

        untracked(() => {
            if (name.trim() && name !== this.#store.draftName()) {
                this.#dispatch.nameChanged(name);
            }

            if (description !== this.#store.draftDescription()) {
                this.#dispatch.descriptionChanged(description);
            }
        });
    });
}
