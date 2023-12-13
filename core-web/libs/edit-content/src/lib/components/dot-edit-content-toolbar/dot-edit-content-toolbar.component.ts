import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';

import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotRenderMode, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-toolbar',
    standalone: true,
    imports: [CommonModule, ToolbarModule, SplitButtonModule],
    providers: [DotWorkflowsActionsService],
    templateUrl: './dot-edit-content-toolbar.component.html',
    styleUrls: ['./dot-edit-content-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentToolbarComponent implements OnInit {
    public items = [
        {
            label: 'Update',
            icon: 'pi pi-refresh',
            command: () => {
                /* this.update(); */
            }
        },
        {
            label: 'Delete',
            icon: 'pi pi-times',
            command: () => {
                /* this.update(); */
            }
        }
    ];

    @Input() inode: string;

    private readonly workflowActionService = inject(DotWorkflowsActionsService);

    ngOnInit(): void {
        this.workflowActionService
            .getByInode(this.inode, DotRenderMode.EDITING)
            .subscribe((_actions) => {
                /* this.items = this.groupActions(actions); */
            });
    }

    private groupActions(actions: DotCMSWorkflowAction[]) {
        return actions.reduce((acc, action) => {
            if (action.metadata.subtype === 'SEPARATOR') {
                acc.push([]);
            }

            const lastGroup = acc[acc.length - 1];
            lastGroup.push(action);

            return acc;
        }, []);

        // console.log(groupedActions);
    }
}
