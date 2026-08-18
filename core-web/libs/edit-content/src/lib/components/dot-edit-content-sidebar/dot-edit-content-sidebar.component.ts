import { ChangeDetectionStrategy, Component, computed, inject, model, output } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { DotEditContentSidebarActivitiesComponent } from './components/dot-edit-content-sidebar-activities/dot-edit-content-sidebar-activities.component';
import { DotEditContentSidebarHistoryComponent } from './components/dot-edit-content-sidebar-history/dot-edit-content-sidebar-history.component';
import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarLocalesSelectorComponent } from './components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales-selector/dot-edit-content-sidebar-locales-selector.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import {
    DotHistoryTimelineItemAction,
    DotWorkflowState
} from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';
import { escapeHtml } from '../../utils/functions.util';

/**
 * The DotEditContentSidebarComponent is a component that displays the sidebar for the DotCMS content editing application.
 * It provides a sidebar with information about the contentlet and workflow actions.
 */
@Component({
    selector: 'dot-edit-content-sidebar',
    templateUrl: './dot-edit-content-sidebar.component.html',
    styleUrl: './dot-edit-content-sidebar.component.scss',
    providers: [ConfirmationService],
    imports: [
        DotMessagePipe,
        DotEditContentSidebarInformationComponent,
        DotEditContentSidebarWorkflowComponent,
        TabsModule,
        TooltipModule,
        DotEditContentSidebarSectionComponent,
        ConfirmDialogModule,
        DialogModule,
        SelectModule,
        ButtonModule,
        DotEditContentSidebarLocalesSelectorComponent,
        DotEditContentSidebarActivitiesComponent,
        DotEditContentSidebarHistoryComponent,
        DotWorkflowActionsComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex h-full flex-col items-start border-l border-surface-200 relative min-w-0 overflow-x-hidden',
        '[style.width.px]': 'sidebarWidth'
    }
})
export class DotEditContentSidebarComponent {
    /** Fixed width of the sidebar panel, in pixels. */
    protected readonly sidebarWidth = 360;

    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $identifier = this.$store.getCurrentContentIdentifier;
    readonly $formValues = this.$store.formValues;
    readonly $contentType = this.$store.contentType;
    readonly $contentlet = this.$store.contentlet;

    // Activities
    readonly $activities = this.$store.activities;
    readonly $initialContentletState = this.$store.initialContentletState;
    readonly $activitiesStatus = computed(() => this.$store.activitiesStatus().status);

    // History properties
    readonly $versionsItems = this.$store.versions; // All accumulated versions for infinite scroll
    readonly $versionsPagination = this.$store.versionsPagination;
    readonly $historyStatus = computed(() => this.$store.versionsStatus().status);
    readonly $pushPublishHistoryItems = this.$store.pushPublishHistory; // All accumulated push publish history items
    readonly $pushPublishHistoryPagination = this.$store.pushPublishHistoryPagination;
    readonly $pushPublishHistoryStatus = computed(
        () => this.$store.pushPublishHistoryStatus().status
    );

    /**
     * Computed property that returns the workflow state of the content.
     */
    readonly $workflow = computed<DotWorkflowState | null>(() => {
        const scheme = this.$store.getScheme();

        // Null until a scheme is resolved — new content has none until the user picks one, and
        // `DotWorkflowState.scheme` is required. The consuming component already declares this
        // input as `DotWorkflowState | null` and renders nothing without it.
        if (!scheme) {
            return null;
        }

        return {
            scheme,
            step: this.$store.getCurrentStep() ?? null,
            task: this.$store.lastTask(),
            contentState: this.$store.initialContentletState(),
            resetAction: this.$store.getResetWorkflowAction()
        };
    });

    /**
     * Computed property that returns the workflow selection state.
     */
    readonly $workflowSelection = computed(() => ({
        schemeOptions: this.$store.workflowSchemeOptions(),
        isWorkflowSelected: this.$store.showSelectWorkflowWarning()
    }));

    /**
     * Model for the showDialog property.
     */
    readonly $showDialog = model<boolean>(false, {
        alias: 'showDialog'
    });

    /**
     * Emits the selected workflow action when the user fires one from the Actions tab,
     * so the parent layout can run it against the edit-content form.
     */
    readonly workflowActionFired = output<DotCMSWorkflowAction>();

    /**
     * Fires the reset-workflow action directly against the store.
     *
     * This deliberately bypasses the form's workflow flow (validation, scroll-to-error,
     * push-publish environment checks, wizard) because the reset action doesn't need them.
     * Every OTHER workflow action must go through the `workflowActionFired` output so the form
     * validates it — do NOT route non-reset actions here, or validation is silently skipped.
     *
     * @param actionId - The ID of the reset workflow action to fire.
     */
    fireResetWorkflowAction(actionId: string): void {
        const contentlet = this.$contentlet();
        const contentType = this.$contentType();

        // A reset action only exists for content that has already entered a workflow, so both are
        // present whenever the button that calls this is rendered. Guarding rather than asserting
        // keeps it that way: firing with an undefined inode would reset the wrong thing.
        if (!contentlet || !contentType) {
            return;
        }

        this.$store.fireWorkflowAction({
            actionId,
            inode: contentlet.inode,
            data: {
                contentlet: {
                    ...this.$formValues(),
                    contentType: contentType.variable
                }
            }
        });
    }

    /**
     * Handles a click on the lock button.
     *
     * - Locked by another user (current user has release permission, since the button is only
     *   rendered when `canLock` is true): ask for confirmation before stealing the lock.
     * - Locked by the current user: unlock directly.
     * - Unlocked: lock it for editing.
     */
    onLockAction(): void {
        if (this.$store.isLockedByAnotherUser()) {
            const lockedBy = this.$store.lockedByName();
            this.#confirmationService.confirm({
                header: this.#dotMessageService.get(
                    'edit.content.release.lock.confirmation.header'
                ),
                message: this.#dotMessageService.get(
                    'edit.content.release.lock.confirmation.message',
                    // PrimeNG renders the confirm message via [innerHTML]; escape the API name.
                    lockedBy ? ` (${escapeHtml(lockedBy)})` : ''
                ),
                acceptLabel: this.#dotMessageService.get('Release-Lock'),
                rejectLabel: this.#dotMessageService.get('Cancel'),
                acceptButtonStyleClass: 'p-button-sm',
                rejectButtonStyleClass: 'p-button-sm p-button-outlined p-button-secondary',
                accept: () => this.$store.unlockContent()
            });

            return;
        }

        if (this.$store.isContentLocked()) {
            this.$store.unlockContent();

            return;
        }

        this.$store.lockContent();
    }

    /**
     * Handles the active index change event from the sidebar tabs.
     * @param value - The index of the active tab
     */
    onActiveIndexChange(value: number | string) {
        const numberValue = Number(value);
        if (isNaN(numberValue)) {
            return;
        }
        this.$store.setActiveSidebarTab(numberValue);
    }

    /**
     * Handles the comment submitted event from the sidebar.
     * @param $event - The event object containing the comment.
     */
    onCommentSubmitted($event: string) {
        const identifier = this.$identifier();

        // Guarded like the three sibling handlers below: no identifier means no saved content to
        // attach a comment to.
        if (!identifier) {
            return;
        }

        this.$store.addComment({
            comment: $event,
            identifier
        });
    }

    /**
     * Handles pagination navigation for version history (automatically detects initial vs accumulation)
     * @param page - The page number to navigate to
     */
    onVersionsPageChange(page: number) {
        const identifier = this.$identifier();
        if (identifier) {
            this.$store.loadVersions({ identifier, page });
        }
    }

    /**
     * Handles pagination navigation for push publish history (automatically detects initial vs accumulation)
     * @param page - The page number to navigate to
     */
    onPushPublishPageChange(page: number) {
        const identifier = this.$identifier();
        if (identifier) {
            this.$store.loadPushPublishHistory({ identifier, page });
        }
    }

    /**
     * Handles timeline item actions from history component
     * @param action - The action object containing type and item data
     */
    onTimelineItemAction(action: DotHistoryTimelineItemAction) {
        this.$store.handleHistoryAction(action);
    }

    /**
     * Handles delete all push publish history action
     * Calls the store method to delete push publish history with confirmation
     */
    onDeletePushPublishHistory() {
        const identifier = this.$identifier();
        if (identifier) {
            this.$store.deletePushPublishHistory(identifier);
        }
    }

    /**
     * Tabs passthrough (pt) configuration for PrimeNG v21 styling.
     * Allows styling PrimeNG's internal elements with Tailwind classes.
     */
    readonly tabsPt = {
        root: { class: 'h-full flex flex-col' },
        nav: { class: 'border-none min-h-12 max-h-[52px]' },
        navContent: { class: 'flex items-stretch w-full gap-3 overflow-visible' },
        panels: {
            class: 'h-[calc(100%-54px)] overflow-auto transition-opacity duration-150 ease-in-out'
        },
        panel: { class: 'h-full' }
    };
}
