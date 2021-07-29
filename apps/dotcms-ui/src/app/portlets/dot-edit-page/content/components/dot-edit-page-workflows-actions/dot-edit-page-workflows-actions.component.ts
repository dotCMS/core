import { Observable } from 'rxjs';
import {
    Component,
    Input,
    OnChanges,
    SimpleChanges,
    Output,
    EventEmitter,
    ChangeDetectionStrategy
} from '@angular/core';
import { tap, map, catchError, pluck, take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';

import { DotCMSContentlet, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotPage } from '@models/dot-page/dot-page.model';

@Component({
    selector: 'dot-edit-page-workflows-actions',
    templateUrl: './dot-edit-page-workflows-actions.component.html',
    styleUrls: ['./dot-edit-page-workflows-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
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
        this.dotWorkflowActionsFireService
            .fireTo(this.page.workingInode, workflow.id, data)
            .pipe(
                take(1),
                catchError((error) => {
                    this.httpErrorManagerService.handle(error);
                    return currentMenuActions;
                })
            )
            .subscribe((contentlet: DotCMSContentlet) => {
                this.dotGlobalMessageService.display(
                    this.dotMessageService.get('editpage.actions.fire.confirmation', workflow.name)
                );
                const newInode = contentlet.inode || this.page.workingInode;
                this.fired.emit(contentlet);
                this.actions = this.getWorkflowActions(newInode);
            });
    }
}
