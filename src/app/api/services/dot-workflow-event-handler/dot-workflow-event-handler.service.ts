import { Injectable } from '@angular/core';
import { catchError, map, take } from 'rxjs/operators';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import {
    DotCMSWorkflowAction,
    DotCMSWorkflowInput,
    DotCMSWorkflowActionEvent
} from 'dotcms-models';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import * as moment from 'moment';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { Observable } from 'rxjs';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotActionBulkRequestOptions } from '@models/dot-action-bulk-request-options/dot-action-bulk-request-options.model';

enum DotActionInputs {
    ASSIGNABLE = 'assignable',
    COMMENTABLE = 'commentable',
    COMMENTANDASSIGN = 'commentAndAssign'
}

const EDIT_CONTENT_CALLBACK_FUNCTION = 'saveAssignCallBackAngular';

@Injectable()
export class DotWorkflowEventHandlerService {
    private workflowStepMap = {
        commentAndAssign: DotCommentAndAssignFormComponent,
        pushPublish: DotPushPublishFormComponent
    };

    constructor(
        private pushPublishService: PushPublishService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotMessageService: DotMessageService,
        private dotWizardService: DotWizardService,
        private dotIframeService: DotIframeService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotGlobalMessageService: DotGlobalMessageService
    ) {}

    /**
     * Fire the event to open the wizard to collect data
     * @param {DotCMSWorkflowActionEvent} event
     * @memberof DotWorkflowEventHandlerService
     */
    open(event: DotCMSWorkflowActionEvent): void {
        if (this.containsPushPublish(event.workflow.actionInputs)) {
            this.checkPublishEnvironments()
                .pipe(take(1))
                .subscribe((hasEnviroments: boolean) => {
                    if (hasEnviroments) {
                        this.openWizard(event);
                    }
                });
        } else {
            this.openWizard(event);
        }
    }

    /**
     * Check if there are environments present otherwise send a notification
     * @returns Observable<boolean>
     * @memberof DotWorkflowEventHandlerService
     */
    checkPublishEnvironments(): Observable<boolean> {
        return this.pushPublishService.getEnvironments().pipe(
            take(1),
            map((environments: DotEnvironment[]) => {
                if (environments.length) {
                    return true;
                } else {
                    this.dotMessageDisplayService.push({
                        life: 3000,
                        message: this.dotMessageService.get(
                            'editpage.actions.fire.error.add.environment'
                        ),
                        severity: DotMessageSeverity.ERROR,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                }
                return false;
            })
        );
    }

    /**
     * Check if Push Publish is par of th sub-actions of the workflow.
     * @param {DotCMSWorkflowInput[]} inputs
     * @returns boolean
     * @memberof DotWorkflowEventHandlerService
     */
    containsPushPublish(inputs: DotCMSWorkflowInput[]): boolean {
        return inputs.some(input => input.id === 'pushPublish');
    }

    /**
     * Returns the input needed to collect information of the Workflow inputs.
     *
     * @param {DotCMSWorkflowAction} workflow
     * @param {string} title
     * @returns DotWizardInput
     * @memberof DotWorkflowEventHandlerService
     */
    setWizardInput(workflow: DotCMSWorkflowAction, title: string): DotWizardInput {
        const steps: DotWizardStep<any>[] = [];
        this.mergeCommentAndAssign(workflow).forEach((input: DotCMSWorkflowInput) => {
            if (this.workflowStepMap[input.id]) {
                steps.push({
                    component: this.workflowStepMap[input.id],
                    data: input.body
                });
            }
        });
        return steps.length
            ? {
                  title: title,
                  steps: steps
              }
            : null;
    }

    /**
     * convert the data collected to what is expecting the endpoint.
     * @param {{ [key: string]: any }} data
     * @param {DotCMSWorkflowInput[]} inputs
     * @returns { [key: string]: any }
     * @memberof DotWorkflowEventHandlerService
     */
    processWorkflowPayload(
        data: { [key: string]: any },
        inputs: DotCMSWorkflowInput[]
    ): { [key: string]: any } {
        if (this.containsPushPublish(inputs)) {
            data['whereToSend'] = data.environment.join();
            data['iWantTo'] = data.pushActionSelected;
            data['publishTime'] = moment(data.publishDate).format('HH-mm');
            data['publishDate'] = moment(data.publishDate).format('YYYY-MM-DD');
            data['expireTime'] = moment(data.expireDate).format('HH-mm');
            data['expireDate'] = moment(data.expireDate).format('YYYY-MM-DD');
            delete data.environment;
            delete data.pushActionSelected;
        }
        return data;
    }

    private mergeCommentAndAssign(workflow: DotCMSWorkflowAction): DotCMSWorkflowInput[] {
        const body = {};
        let workflows: DotCMSWorkflowInput[];
        workflow.actionInputs.forEach(input => {
            if (this.isCommentOrAssign(input.id)) {
                body[input.id] = true;
            }
        });
        if (Object.keys(body).length) {
            workflows = workflow.actionInputs.filter(input => !this.isCommentOrAssign(input.id));
            workflows.unshift({
                id: DotActionInputs.COMMENTANDASSIGN,
                body: { ...body, ...this.getAssignableData(workflow) }
            });
        } else {
            return workflow.actionInputs;
        }
        return workflows;
    }

    private openWizard(event: DotCMSWorkflowActionEvent): void {
        this.dotWizardService
            .open(
                this.setWizardInput(event.workflow, this.dotMessageService.get('Workflow-Action'))
            )
            .pipe(take(1))
            .subscribe((data: { [key: string]: any }) => {
                this.fireWorkflowAction(event, data);
            });
    }

    private fireWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        data?: { [key: string]: any }
    ): void {
        if (this.isBulkAction(event)) {
            this.dotIframeService.run({ name: 'fireActionLoadingIndicator' });
            this.dotWorkflowActionsFireService
                .bulkFire(this.processBulkData(event, data))
                .pipe(
                    take(1),
                    catchError(error => {
                        return this.httpErrorManagerService.handle(error);
                    })
                )
                .subscribe((response: DotActionBulkResult) => {
                    this.displayNotification(event.workflow.name);
                    this.dotIframeService.run({ name: event.callback, args: [response] });
                });
        } else {
            if (event.callback === EDIT_CONTENT_CALLBACK_FUNCTION) {
                // path for create or edit content since we don't have the iNode.
                this.dotIframeService.run({
                    name: event.callback,
                    args: [
                        event.workflow.id,
                        this.processWorkflowPayload(data, event.workflow.actionInputs)
                    ]
                });
            } else {
                this.dotWorkflowActionsFireService
                    .fireTo(
                        event.inode,
                        event.workflow.id,
                        this.processWorkflowPayload(data, event.workflow.actionInputs)
                    )
                    .pipe(
                        catchError(error => {
                            return this.httpErrorManagerService.handle(error);
                        }),
                        take(1)
                    )
                    .subscribe(() => {
                        this.displayNotification(event.workflow.name);
                        this.dotIframeService.run({ name: event.callback });
                    });
            }
        }
    }

    private isBulkAction(event: DotCMSWorkflowActionEvent): boolean {
        return !!(event.selectedInodes && event.selectedInodes.length);
    }

    private displayNotification(name: string): void {
        this.dotGlobalMessageService.display(
            this.dotMessageService.get('editpage.actions.fire.confirmation', name)
        );
    }

    private getAssignableData(workflow: DotCMSWorkflowAction): { [key: string]: any } {
        return { roleId: workflow.nextAssign, roleHierarchy: workflow.roleHierarchyForAssign };
    }

    private isCommentOrAssign(id: string): boolean {
        return id === DotActionInputs.ASSIGNABLE || id === DotActionInputs.COMMENTABLE;
    }

    private processBulkData(
        event: DotCMSWorkflowActionEvent,
        data?: { [key: string]: any }
    ): DotActionBulkRequestOptions {
        data = this.processWorkflowPayload(data, event.workflow.actionInputs);
        const requestOptions: DotActionBulkRequestOptions = {
            workflowActionId: event.workflow.id,
            additionalParams: {
                assignComment: {
                    comment: data.comments,
                    assign: data.assign
                },
                pushPublish: {
                    whereToSend: data.whereToSend,
                    iWantTo: data.iWantTo,
                    expireDate: data.expireDate,
                    expireTime: data.expireTime,
                    publishDate: data.publishDate,
                    publishTime: data.publishTime,
                    filterKey: data.filterKey
                }
            }
        };
        if (Array.isArray(event.selectedInodes)) {
            requestOptions['contentletIds'] = event.selectedInodes;
        } else {
            requestOptions['query'] = event.selectedInodes;
        }
        return requestOptions;
    }
}
