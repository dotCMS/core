import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';

import { DotEditContentSidebarLocalesComponent } from '@dotcms/edit-content/components/dot-edit-content-sidebar/components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales.component';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DotWorkflowState } from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';

/**
 * The DotEditContentSidebarComponent is a component that displays the sidebar for the DotCMS content editing application.
 * It provides a sidebar with information about the contentlet and workflow actions.
 */
@Component({
    selector: 'dot-edit-content-sidebar',
    standalone: true,
    templateUrl: './dot-edit-content-sidebar.component.html',
    styleUrls: ['./dot-edit-content-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotMessagePipe,
        DotEditContentSidebarInformationComponent,
        DotEditContentSidebarWorkflowComponent,
        TabViewModule,
        TabViewInsertDirective,
        DotEditContentSidebarSectionComponent,
        DotCopyButtonComponent,

        DialogModule,
        DropdownModule,
        ButtonModule,
        DotEditContentSidebarLocalesComponent
    ]
})
export class DotEditContentSidebarComponent {
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly $identifier = this.$store.getCurrentContentIdentifier;
    readonly $formValues = this.$store.formValues;
    readonly $contentType = this.$store.contentType;
    readonly $contentlet = this.$store.contentlet;

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
}
