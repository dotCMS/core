import { SlicePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    model,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TabViewModule } from 'primeng/tabview';

import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';

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
        SlicePipe,
        DialogModule,
        DropdownModule,
        ButtonModule
    ]
})
export class DotEditContentSidebarComponent {
    readonly store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly $identifier = this.store.getCurrentContentIdentifier;

    /**
     * Model for the showDialog property.
     */
    readonly $showDialog = model<boolean>(false, {
        alias: 'showDialog'
    });

    /**
     * Effect that triggers the workflow status and new content status based on the contentlet and content type ID.
     */
    #workflowEffect = effect(() => {
        const inode = this.store.contentlet()?.inode;

        untracked(() => {
            if (inode) {
                this.store.getWorkflowStatus(inode);
            }
        });
    });

    /**
     * Effect that triggers the reference pages based on the contentlet identifier.
     */
    #informationEffect = effect(() => {
        const identifier = this.$identifier();

        untracked(() => {
            if (identifier) {
                this.store.getReferencePages(identifier);
            }
        });
    });
}
