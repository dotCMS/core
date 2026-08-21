import { injectDispatch } from '@ngrx/signals/events';

import { effect, EffectRef, Signal, untracked } from '@angular/core';

import { DotExperimentPatchBody } from '@dotcms/dotcms-models';

import { hasPendingChanges } from './dot-experiments-configure.util';

import { dotExperimentsConfigurePageEvents } from '../store/dot-experiments-configure-page.events';

/** How an edited form becomes an autosaved PATCH. */
export interface FormAutosaveBinding<T> {
    /** The form value as edited on screen. The only tracked dependency of the binding. */
    model: Signal<T>;
    /**
     * The keys the form holds that the store does not. An empty body is not a save.
     *
     * Called untracked, so it is free to read whatever it needs to diff against — a save response
     * must not re-trigger a save.
     */
    toPatch: (value: T) => DotExperimentPatchBody;
}

/**
 * Dispatches `formEdited` whenever the form holds something the store has not been told about.
 *
 * There is one binding for the whole screen, over the one root form, so the diff is computed in one
 * place from one model. It replaced five per-field bindings, each of which had to remember the
 * `untracked` that stops a save response from bouncing straight back as another save — and each of
 * which knew only about its own key, which is not how the PATCH endpoint works.
 *
 * The comparison is the whole point: the store applies every reported edit locally and re-seeds
 * itself from every response, so a binding that dispatched on any model change would turn one edit
 * into an endless round trip.
 *
 * The debounce lives in the store, not here (AC6): the screen accumulates one diff and flushes it
 * on one timer, which is what makes two cards edited in the same window a single call.
 *
 * Must be called from an injection context — a field initializer or a constructor — since it
 * registers an effect and injects the dispatcher.
 *
 * @returns The effect, so the caller can hold it in a field like any other.
 */
export function bindFormAutosave<T>({ model, toPatch }: FormAutosaveBinding<T>): EffectRef {
    const dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    return effect(() => {
        const value = model();

        untracked(() => {
            const patch = toPatch(value);

            if (!hasPendingChanges(patch)) {
                return;
            }

            dispatch.formEdited(patch);
        });
    });
}
