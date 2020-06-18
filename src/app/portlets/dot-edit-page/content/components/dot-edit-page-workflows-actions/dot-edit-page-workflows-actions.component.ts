import { of as observableOf, Observable } from 'rxjs';
import { Component, Input, OnChanges, SimpleChanges, Output, EventEmitter } from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotCMSWorkflowAction } from 'dotcms-models';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPage } from '../../../shared/models/dot-page.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { tap, map, mergeMap, catchError, pluck } from 'rxjs/operators';
import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';

@Component({
    selector: 'dot-edit-page-workflows-actions',
    templateUrl: './dot-edit-page-workflows-actions.component.html',
    styleUrls: ['./dot-edit-page-workflows-actions.component.scss']
})
export class DotEditPageWorkflowsActionsComponent implements OnChanges {
    @Input() page: DotPage;

    @Output() fired: EventEmitter<any> = new EventEmitter();

    actionsAvailable: boolean;
    actions: Observable<MenuItem[]>;

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotMessageService: DotMessageService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotGlobalMessageService: DotGlobalMessageService
    ) {}

    ngOnChanges(changes: SimpleChanges) {
        if (changes.page) {
            this.actions = this.getWorkflowActions(this.page.workingInode);
        }
    }

    private getWorkflowActions(inode: string): Observable<MenuItem[]> {
        return this.dotWorkflowsActionsService.getByInode(inode).pipe(
            tap((workflows: DotCMSWorkflowAction[]) => {
                this.actionsAvailable = !!workflows.length;
            }),
            map((newWorkflows: DotCMSWorkflowAction[]) => {
                return newWorkflows.length !== 0 ? this.getWorkflowOptions(newWorkflows) : [];
            })
        );
    }

    private getWorkflowOptions(workflows: DotCMSWorkflowAction[]): MenuItem[] {
        return workflows.map((workflow: DotCMSWorkflowAction) => {
            return {
                label: workflow.name,
                command: () => {
                    const currentMenuActions = this.actions;
                    this.actions = this.dotWorkflowActionsFireService
                        .fireTo(this.page.workingInode, workflow.id)
                        .pipe(
                            pluck('inode'),
                            tap(() => {
                                this.dotGlobalMessageService.display(
                                    this.dotMessageService.get(
                                        'editpage.actions.fire.confirmation',
                                        workflow.name
                                    )
                                );
                            }),
                            // TODO: A better implementation needs to be done to handle workflow actions errors, which are edge cases
                            catchError(() => observableOf(null)),
                            mergeMap((inode: string) => {
                                const newInode = inode || this.page.workingInode;
                                this.fired.emit();
                                return this.getWorkflowActions(newInode);
                            }),
                            catchError(error => {
                                this.httpErrorManagerService.handle(error);
                                return currentMenuActions;
                            })
                        );
                }
            };
        });
    }
}
