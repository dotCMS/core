import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    output,
    untracked
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { DotEditContentSidebarActivitiesComponent } from './components/dot-edit-content-sidebar-activities/dot-edit-content-sidebar-activities.component';
import { DotEditContentSidebarHistoryComponent } from './components/dot-edit-content-sidebar-history/dot-edit-content-sidebar-history.component';
import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarLocalesComponent } from './components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import {
    DotHistoryTimelineItemAction,
    DotWorkflowState
} from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';

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
        DotEditContentSidebarLocalesComponent,
        DotEditContentSidebarActivitiesComponent,
        DotEditContentSidebarHistoryComponent,
        DotWorkflowActionsComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex w-[360px] h-full flex-col items-start border-l border-surface-200 relative min-w-0 overflow-x-hidden'
    }
})
export class DotEditContentSidebarComponent {
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
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
    readonly $workflow = computed<DotWorkflowState>(() => ({
        scheme: this.$store.getScheme(),
        step: this.$store.getCurrentStep(),
        task: this.$store.lastTask(),
        contentState: this.$store.initialContentletState(),
        resetAction: this.$store.getResetWorkflowAction()
    }));

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
     * Effect that loads sidebar data (reference pages and activities) when the
     * sidebar is open and the contentlet identifier is available.
     * Gating on `isSidebarOpen` avoids firing these API calls on every edit-content
     * page load when the user never actually opens the sidebar.
     */
    #informationEffect = effect(() => {
        const identifier = this.$identifier();
        const isSidebarOpen = this.$store.isSidebarOpen();

        untracked(() => {
            if (identifier && isSidebarOpen) {
                this.$store.getReferencePages(identifier);
                this.$store.loadActivities(identifier);
            }
        });
    });

    /**
     * Fires a workflow action.
     *
     * NOTE: this fires the action straight against the store, intentionally bypassing the
     * form's `fireWorkflowAction` (validation, scroll-to-error, push-publish environment
     * checks, wizard flow). It is currently only used for the sidebar reset-workflow action,
     * which doesn't need form validation.
     * TODO(#35892): align with the form's flow if non-reset actions are ever fired from here.
     *
     * @param actionId - The ID of the action to fire.
     */
    fireWorkflowAction(actionId: string): void {
        this.$store.fireWorkflowAction({
            actionId,
            inode: this.$contentlet().inode,
            data: {
                contentlet: {
                    ...this.$formValues(),
                    contentType: this.$contentType().variable
                }
            }
        });
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
        nav: { class: 'border-none min-h-[50px] max-h-[52px]' },
        navContent: { class: 'flex items-stretch w-full gap-3 overflow-visible' },
        panels: {
            class: 'h-[calc(100%-54px)] overflow-auto transition-opacity duration-150 ease-in-out'
        },
        panel: { class: 'h-full' }
    };
}
