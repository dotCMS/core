import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/**
 * Persistent hint naming what Save draft covers.
 *
 * Deliberately specific rather than "nothing is saved until you press Save": adding, renaming and
 * deleting a variant have their own endpoints and persist the moment they happen, so a blanket
 * claim would be untrue for the card the user is most likely to be looking at.
 */
const SAVE_HINT_KEY = 'experiments.configure.footer.save-hint';

/** Replaces the hint once there is something to save, so the button reads as the way out. */
const UNSAVED_HINT_KEY = 'experiments.configure.footer.unsaved';

/** Replaces the hint while the save is in flight, so the write is not invisible. */
const SAVING_HINT_KEY = 'experiments.configure.footer.saving';

/** Replaces the hint once the configuration is read-only, where nothing is being saved. */
const LOCKED_HINT_KEY = 'experiments.configure.footer.locked';

/** Count of fields that failed the Start check; the singular reads better as its own message. */
const VALIDATION_HINT_KEY_ONE = 'experiments.configure.footer.validation.one';
const VALIDATION_HINT_KEY_MANY = 'experiments.configure.footer.validation.many';

/** What the footer says, and whether it says it as an error. */
interface FooterHint {
    key: string;
    /** Message arguments, e.g. the failing-field count. */
    args: string[];
    isError: boolean;
}

/**
 * Footer of the Configure screen: the autosave hint on the left, and the way out plus the
 * Start/Schedule transition on the right.
 *
 * The store is injected rather than received through inputs — it is provided by the Configure
 * shell, so this component reads the same instance every card writes to.
 *
 * Start is never disabled (AC28): pressing it with an incomplete form is what *reveals* the
 * errors, and the count lands in the hint. The scroll to the first failing field is the shell's,
 * which owns the scrolling region and already reacts to `validationErrors` — this component only
 * dispatches and reports.
 */
@Component({
    selector: 'dot-experiments-configure-footer',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-footer.component.html',
    host: {
        class: 'flex flex-none items-center justify-between gap-4 border-t border-surface-200 bg-surface-50 px-8 py-3'
    }
})
export class DotExperimentsConfigureFooterComponent {
    readonly store = inject(DotExperimentsConfigureStore);

    /**
     * The single line of copy on the left, in precedence order: a locked experiment is not saving
     * anything, a failed Start is the most recent thing the user did, a save in flight is worth
     * showing over anything static, and unsaved work is worth saying before the generic hint.
     */
    readonly $hint = computed<FooterHint>(() => {
        if (this.store.$isLocked()) {
            return { key: LOCKED_HINT_KEY, args: [], isError: false };
        }

        const errorCount = this.store.$validationErrorCount();

        if (errorCount > 0) {
            return {
                key: errorCount === 1 ? VALIDATION_HINT_KEY_ONE : VALIDATION_HINT_KEY_MANY,
                args: [String(errorCount)],
                isError: true
            };
        }

        if (this.store.$isSaving()) {
            return { key: SAVING_HINT_KEY, args: [], isError: false };
        }

        return {
            key: this.store.$hasUnsavedChanges() ? UNSAVED_HINT_KEY : SAVE_HINT_KEY,
            args: [],
            isError: false
        };
    });

    /**
     * Save draft is offered while the experiment can still be edited. It is the only thing that
     * writes the form, so it outlives the Start button — a draft may be saved and left alone.
     */
    readonly $showSave = computed<boolean>(() => !this.store.$isLocked());

    /**
     * Nothing to write, or a write already on its way.
     *
     * `$canSave` rather than `$hasUnsavedChanges`: with the weights mid-edit the form is dirty —
     * the leave prompt still fires — but the body would be one the backend refuses, and a button
     * that produces a 400 reads as broken. The weights warning beside the rows says why.
     */
    readonly $isSaveDisabled = computed<boolean>(
        () => !this.store.$canSave() || this.store.$isSaving()
    );

    /**
     * A start dated in the future schedules the experiment instead of running it, and the button
     * has to say which of the two the press does (AC32).
     */
    readonly $startLabelKey = computed<string>(() =>
        this.store.$isScheduledStart()
            ? 'experiments.action.schedule-experiment'
            : 'experiments.action.start-experiment'
    );

    /**
     * Only a draft can be started. Every other status either is already running or has been
     * stopped, and a scheduled one is cancelled from the header's kebab rather than re-started.
     */
    readonly $showStart = computed<boolean>(
        () => this.store.$status() === DotExperimentStatus.DRAFT
    );

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    /** Writes the form: creates the experiment on the first press, patches it afterwards. */
    onSaveDraft(): void {
        this.#dispatch.saveDraftRequested();
    }

    /**
     * Asks the store to start — or, on an incomplete form, to reveal what is missing.
     *
     * There is no guard here on purpose: the store's reducer runs the eight rules on this event,
     * and only lets the call through when none of them failed (AC29/AC30).
     */
    onStart(): void {
        this.#dispatch.startRequested();
    }
}
