import { Observable } from 'rxjs';
import { Component, Input, OnChanges, SimpleChanges, Output, EventEmitter } from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotCMSWorkflowAction } from 'dotcms-models';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPage } from '../../../shared/models/dot-page.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { tap, map, mergeMap, catchError, pluck, take } from 'rxjs/operators';
import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';

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
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotWizardService: DotWizardService,
        private dotWorkflowEventHandlerService: DotWorkflowEventHandlerService
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
                    if (workflow.actionInputs.length) {
                        if (
                            this.dotWorkflowEventHandlerService.containsPushPublish(
                                workflow.actionInputs
                            )
                        ) {
                            this.dotWorkflowEventHandlerService
                                .checkPublishEnvironments()
                                .pipe(take(1))
                                .subscribe((hasEnviroments: boolean) => {
                                    if (hasEnviroments) {
                                        this.openWizard(workflow);
                                    }
                                });
                        } else {
                            this.openWizard(workflow);
                        }
                    } else {
                        this.fireWorkflowAction(workflow);
                    }
                }
            };
        });
    }

    private openWizard(workflow: DotCMSWorkflowAction): void {
        this.dotWizardService
            .open(
                this.dotWorkflowEventHandlerService.setWizardInput(
                    workflow,
                    this.dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: { [key: string]: any }) => {
                this.fireWorkflowAction(
                    workflow,
                    this.dotWorkflowEventHandlerService.processWorkflowPayload(
                        data,
                        workflow.actionInputs
                    )
                );
            });
    }

    private fireWorkflowAction(
        workflow: DotCMSWorkflowAction,
        data?: { [key: string]: any }
    ): void {
        const currentMenuActions = this.actions;
        this.actions = this.dotWorkflowActionsFireService
            .fireTo(this.page.workingInode, workflow.id, data)
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
}
