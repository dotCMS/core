import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowAction, DotWorkflowPayload } from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-workflow-actions',
    standalone: true,
    imports: [AsyncPipe, DotWorkflowActionsComponent],
    templateUrl: './dot-edit-ema-workflow-actions.component.html',
    styleUrl: './dot-edit-ema-workflow-actions.component.css'
})
export class DotEditEmaWorkflowActionsComponent implements OnChanges {
    @Input({ required: true }) inodeOrIdentifier: string;
    @Output() fired: EventEmitter<DotCMSContentlet> = new EventEmitter();
    actions$: Observable<DotCMSWorkflowAction[]>;

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotMessageService: DotMessageService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotWizardService: DotWizardService,
        private dotWorkflowEventHandlerService: DotWorkflowEventHandlerService
    ) {}

    ngOnChanges(changes: SimpleChanges) {
        if (changes.inodeOrIdentifier) {
            this.actions$ = this.getWorkflowActions(this.inodeOrIdentifier);
        }
    }

    handleActionTrigger(workflow: DotCMSWorkflowAction): void {
        const { actionInputs = [] } = workflow;
        const isPushPublish = this.dotWorkflowEventHandlerService.containsPushPublish(actionInputs);

        if (!actionInputs.length) {
            this.fireWorkflowAction(workflow);

            return;
        }

        if (!isPushPublish) {
            this.openWizard(workflow);

            return;
        }

        this.dotWorkflowEventHandlerService
            .checkPublishEnvironments()
            .pipe(take(1))
            .subscribe((hasEnviroments: boolean) => {
                if (hasEnviroments) {
                    this.openWizard(workflow);
                }
            });
    }

    private getWorkflowActions(inode: string): Observable<DotCMSWorkflowAction[]> {
        return this.dotWorkflowsActionsService.getByInode(inode).pipe(
            map((newWorkflows: DotCMSWorkflowAction[]) => {
                return newWorkflows || [];
            })
        );
    }

    private openWizard(workflow: DotCMSWorkflowAction): void {
        this.dotWizardService
            .open<DotWorkflowPayload>(
                this.dotWorkflowEventHandlerService.setWizardInput(
                    workflow,
                    this.dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: DotWorkflowPayload) => {
                this.fireWorkflowAction(
                    workflow,
                    this.dotWorkflowEventHandlerService.processWorkflowPayload(
                        data,
                        workflow.actionInputs
                    )
                );
            });
    }

    private fireWorkflowAction<T = { [key: string]: string }>(
        workflow: DotCMSWorkflowAction,
        data?: T
    ): void {
        this.dotWorkflowActionsFireService
            .fireTo({
                inode: this.inodeOrIdentifier,
                actionId: workflow.id,
                data
            })
            .pipe(
                take(1),
                catchError((error) => {
                    return this.httpErrorManagerService.handle(error);
                })
            )
            .subscribe((contentlet: DotCMSContentlet) => {
                const inode = contentlet?.inode;
                this.fired.emit(contentlet);
                if (inode !== this.inodeOrIdentifier) {
                    this.actions$ = this.getWorkflowActions(inode);
                }
            });
    }
}
