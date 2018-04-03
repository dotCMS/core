import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { MenuItem } from 'primeng/primeng';
import { DotWorkflowAction } from '../../../../../shared/models/dot-workflow-action/dot-workflow-action.model';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPage } from '../../../shared/models/dot-page.model';

@Component({
    selector: 'dot-edit-page-workflows-actions',
    templateUrl: './dot-edit-page-workflows-actions.component.html',
    styleUrls: ['./dot-edit-page-workflows-actions.component.scss']
})
export class DotEditPageWorkflowsActionsComponent implements OnChanges {
    @Input() page: DotPage;
    @Input() label: string;

    actionsAvailable: boolean;
    workflowsMenuActions: Observable<MenuItem[]>;

    constructor(private workflowsService: DotWorkflowService, private httpErrorManagerService: DotHttpErrorManagerService) {}

    ngOnChanges(changes: SimpleChanges) {
        if (changes.page) {
            this.workflowsMenuActions = this.getWorkflowActions(this.page.workingInode);
        }
    }

    private getWorkflowActions(inode: string): Observable<MenuItem[]> {
        return this.workflowsService
            .getContentWorkflowActions(inode)
            .do((workflows: DotWorkflowAction[]) => {
                this.actionsAvailable = !!workflows.length;
            })
            .map((newWorkflows: DotWorkflowAction[]) => {
                if (newWorkflows.length === 0) {
                    throw {
                        bodyJsonObject: {
                            error: ''
                        },
                        response: {
                            status: 404
                        }
                    };
                }
                return this.getWorkflowOptions(newWorkflows);
            });
    }

    private getWorkflowOptions(workflows: DotWorkflowAction[]): MenuItem[] {
        return workflows.map((workflow: DotWorkflowAction) => {
            return {
                label: workflow.name,
                command: () => {
                    const currentMenuActions = this.workflowsMenuActions;
                    this.workflowsMenuActions = this.workflowsService
                        .fireWorkflowAction(this.page.workingInode, workflow.id)
                        .pluck('inode')
                        // TODO: A better implementation needs to be done to
                        // handle workflow actions errors, which are edge cases
                        .catch(() => Observable.of(null))
                        .mergeMap((inode: string) => {
                            const newInode = inode || this.page.workingInode;
                            return this.getWorkflowActions(newInode);
                        })
                        .catch((error) => {
                            this.httpErrorManagerService.handle(error);
                            return currentMenuActions;
                        });
                }
            };
        });
    }
}
