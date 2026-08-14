import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { EXPERIMENTS_URL } from '../../../shared/constants';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/** Persistent hint that explains why the screen has no Save button (AC8). */
const AUTOSAVE_HINT_KEY = 'experiments.configure.footer.autosave-hint';

/** Replaces the hint while a field group is being persisted, so the writes are not invisible. */
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
     * anything, a failed Start is the most recent thing the user did, and an in-flight autosave is
     * worth showing over the generic hint it is the proof of.
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

        return {
            key: this.store.$isAutosaving() ? SAVING_HINT_KEY : AUTOSAVE_HINT_KEY,
            args: [],
            isError: false
        };
    });

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
    readonly #router = inject(Router);

    /** Leaves the Configure screen for the list. */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL]);
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
