import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { CONFIGURATION_CONFIRM_DIALOG_KEY } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotMessagePipe } from '@dotcms/ui';

import { EXPERIMENTS_URL, STATUS_LABEL_KEYS, STATUS_SEVERITIES } from '../../../shared/constants';
import { TagSeverity } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/** Title shown while the draft has no name yet. */
const NEW_EXPERIMENT_TITLE_KEY = 'experiments.configure.header.new-experiment';

/** Subline shown while no page is selected. */
const NO_PAGE_SELECTED_KEY = 'experiments.configure.header.no-page';

/** Separator between the page title and its path in the subline. */
const SUBLINE_SEPARATOR = ' · ';

/**
 * Header of the Configure screen: back, title, status, the page the experiment runs on, and the
 * actions that apply to the current status.
 *
 * The store is injected rather than received through inputs — it is provided by the Configure
 * shell, so every card on the screen reads the same instance and none of them has to be re-wired
 * when a new field is added.
 *
 * Actions are gated purely on `AllowedActionsByExperimentStatus` through the store's
 * `$allowedActions`; there is no license gate, since dotCMS no longer ships Enterprise licensing.
 * Confirmations are raised on the shell's `p-confirmDialog` by key — this component never renders
 * a dialog of its own — and toasts belong to the shell, which listens for the API events.
 */
@Component({
    selector: 'dot-experiments-configure-header',
    imports: [
        ButtonModule,
        MenuModule,
        TagModule,
        TooltipModule,
        DotAddToBundleComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-configure-header.component.html',
    host: {
        class: 'flex flex-none items-center justify-between gap-6 border-b border-surface-200 bg-white px-8 py-4'
    }
})
export class DotExperimentsConfigureHeaderComponent {
    readonly store = inject(DotExperimentsConfigureStore);

    /** Name as typed, so the title follows the Details card without waiting for the autosave. */
    readonly $title = computed<string>(() => {
        const name = this.store.draftName().trim();

        return name || this.#dotMessageService.get(NEW_EXPERIMENT_TITLE_KEY);
    });

    readonly $statusSeverity = computed<TagSeverity>(
        () => STATUS_SEVERITIES[this.store.$status()] ?? 'secondary'
    );

    readonly $statusLabelKey = computed<string>(
        () => STATUS_LABEL_KEYS.get(this.store.$status()) ?? ''
    );

    /** `{pageTitle} · {pagePath}`, or the "no page" copy while nothing is selected (AC36). */
    readonly $subline = computed<string>(() => {
        const page = this.store.selectedPage();

        if (!page) {
            return this.#dotMessageService.get(NO_PAGE_SELECTED_KEY);
        }

        return [page.title, page.path].filter(Boolean).join(SUBLINE_SEPARATOR);
    });

    /** Results exist for RUNNING and ENDED experiments only. */
    readonly $showResults = computed<boolean>(() => this.store.$allowedActions().results);

    /** Stopping is the RUNNING-only transition; `end` is its entry in the allowed-actions map. */
    readonly $showStop = computed<boolean>(() => this.store.$allowedActions().end);

    /**
     * Kebab actions for the current status.
     *
     * Rebuilt whenever the status changes rather than on every popup toggle: there is a single
     * experiment on this screen, so the menu is a pure function of the state the store already
     * exposes. Everything here needs a persisted experiment, so the whole menu is empty until one
     * exists — the template hides the trigger in that case.
     */
    readonly $menuItems = computed<MenuItem[]>(() => {
        const experiment = this.store.experiment();

        if (!experiment) {
            return [];
        }

        const allowed = this.store.$allowedActions();

        return [
            {
                id: 'experiments-configure-end',
                label: this.#dotMessageService.get('experiments.action.end-experiment'),
                visible: allowed.end,
                command: () => this.confirmStop()
            },
            {
                id: 'experiments-configure-abort',
                label: this.#dotMessageService.get('experiments.action.abort.experiment'),
                visible: allowed.abort,
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.action.abort.experiment',
                        messageKey: 'experiments.action.abort.confirm.message',
                        acceptLabelKey: 'experiments.action.abort.experiment',
                        accept: () => this.#dispatch.abortRequested()
                    })
            },
            {
                id: 'experiments-configure-cancel-schedule',
                label: this.#dotMessageService.get('experiments.configure.scheduling.cancel'),
                visible: allowed.cancelSchedule,
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.configure.scheduling.cancel',
                        messageKey: 'experiments.action.cancel.schedule-confirm',
                        acceptLabelKey: 'dot.common.dialog.accept',
                        accept: () => this.#dispatch.cancelScheduleRequested()
                    })
            },
            {
                id: 'experiments-configure-push-publish',
                label: this.#dotMessageService.get('contenttypes.content.push_publish'),
                visible: allowed.pushPublish,
                command: () =>
                    this.#pushPublishDialogService.open({
                        assetIdentifier: experiment.id,
                        title: this.#dotMessageService.get('contenttypes.content.push_publish')
                    })
            },
            {
                id: 'experiments-configure-add-to-bundle',
                label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
                visible: allowed.addToBundle,
                command: () => this.$addToBundleAssetId.set(experiment.id)
            }
        ];
    });

    /** True once at least one kebab entry applies, so the trigger is not an empty popup. */
    readonly $hasMenuItems = computed<boolean>(() =>
        this.$menuItems().some(({ visible }) => visible)
    );

    /** Identifier of the experiment being added to a bundle, or `null` when the dialog is closed. */
    readonly $addToBundleAssetId = signal<string | null>(null);

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #router = inject(Router);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #pushPublishDialogService = inject(DotPushPublishDialogService);

    /** Leaves the Configure screen for the list. */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL]);
    }

    /**
     * Asks before ending the experiment, then hands the transition to the store.
     *
     * Shared with the kebab's End entry: the header button and the menu item are two ways into the
     * same RUNNING → ENDED transition, and the API has one endpoint for it (AC33).
     */
    confirmStop(): void {
        this.#confirm({
            headerKey: 'experiments.action.end-experiment',
            messageKey: 'experiments.action.stop.delete-confirm',
            acceptLabelKey: 'experiments.action.end',
            accept: () => this.#dispatch.stopRequested()
        });
    }

    /** Raises the confirmation on the shell's dialog — this component renders none of its own. */
    #confirm({
        headerKey,
        messageKey,
        acceptLabelKey,
        accept
    }: {
        headerKey: string;
        messageKey: string;
        acceptLabelKey: string;
        accept: () => void;
    }): void {
        this.#confirmationService.confirm({
            key: CONFIGURATION_CONFIRM_DIALOG_KEY,
            header: this.#dotMessageService.get(headerKey),
            message: this.#dotMessageService.get(messageKey),
            acceptLabel: this.#dotMessageService.get(acceptLabelKey),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept
        });
    }
}
