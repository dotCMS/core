import { SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';

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
        SlicePipe
    ]
})
export class DotEditContentSidebarComponent {
    readonly store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly $identifier = this.store.getCurrentContentIdentifier;

    #workflowEffect = effect(() => {
        const inode = this.store.contentlet()?.inode;
        const contentTypeId = this.store.contentType()?.id;

        untracked(() => {
            if (inode) {
                this.store.getWorkflowStatus(inode);
            }
            if (contentTypeId) {
                this.store.getNewContentStatus(contentTypeId);
            }
        });
    });

    #informationEffect = effect(() => {
        const identifier = this.$identifier();

        untracked(() => {
            if (identifier) {
                this.store.getReferencePages(identifier);
            }
        });
    });
}
