import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    untracked
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';

import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentSidebarActivitiesComponent } from './components/dot-edit-content-sidebar-activities/dot-edit-content-sidebar-activities.component';
import { DotEditContentSidebarHistoryComponent } from './components/dot-edit-content-sidebar-history/dot-edit-content-sidebar-history.component';
import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarLocalesComponent } from './components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import {
    DotWorkflowState,
    DotHistoryTimelineItemAction
} from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';

/**
 * The DotEditContentSidebarComponent is a component that displays the sidebar for the DotCMS content editing application.
 * It provides a sidebar with information about the contentlet and workflow actions.
 */
@Component({
    selector: 'dot-edit-content-sidebar',
    templateUrl: './dot-edit-content-sidebar.component.html',
    styleUrls: ['./dot-edit-content-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [ConfirmationService],
    imports: [
        DotMessagePipe,
        DotEditContentSidebarInformationComponent,
        DotEditContentSidebarWorkflowComponent,
        TabViewModule,
        TabViewInsertDirective,
        DotEditContentSidebarSectionComponent,
        DotCopyButtonComponent,
        ConfirmDialogModule,
        DialogModule,
        DropdownModule,
        ButtonModule,
        DotEditContentSidebarLocalesComponent,
        DotEditContentSidebarActivitiesComponent,
        DotEditContentSidebarHistoryComponent
    ]
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
     * Effect that triggers the reference pages based on the contentlet identifier.
     */
    #informationEffect = effect(() => {
        const identifier = this.$identifier();

        untracked(() => {
            if (identifier) {
                this.$store.getReferencePages(identifier);
                this.$store.loadActivities(identifier);
            }
        });
    });

    /**
     * Fires a workflow action.
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
     * @param $event - The event object containing the active index.
     */
    onActiveIndexChange($event: TabViewChangeEvent) {
        const { index } = $event;
        this.$store.setActiveSidebarTab(index);
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
}
