import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

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
    @Input() inode: string;
    @Input() formValue: Record<string, string>;

    private _grouppedActions: MenuItem[][] = [];
    private readonly workflowActionService = inject(DotWorkflowsActionsService);
    private readonly WorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly cdRef = inject(ChangeDetectorRef);

    get groupedActions(): MenuItem[][] {
        return this._grouppedActions;
    }

    ngOnInit(): void {
        this.workflowActionService
            .getByInode(this.inode, DotRenderMode.EDITING)
            .subscribe((actions) => {
                this._grouppedActions = this.groupActions(actions);
                this.cdRef.markForCheck();
            });
    }

    private groupActions(actions: DotCMSWorkflowAction[]): MenuItem[][] {
        return actions
            .reduce(
                (acc, action) => {
                    if (action?.metadata?.subtype === DotCMSActionSubtype.SEPARATOR) {
                        acc.push([]);
                    } else {
                        acc[acc.length - 1].push({
                            label: action.name,
                            command: () => this.fireAction(action)
                        });
                    }

                    return acc;
                },
                [[]]
            )
            .filter((group) => group.length);
    }

    private fireAction(action: DotCMSWorkflowAction): void {
        this.WorkflowActionsFireService.fireTo(this.inode, action.id, {
            contentlet: {
                ...this.formValue
            }
        }).subscribe();
    }
}
