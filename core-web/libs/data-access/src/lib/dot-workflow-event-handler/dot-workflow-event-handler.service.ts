import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotActionBulkRequestOptions,
    DotActionBulkResult,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotCMSWorkflowInput,
    DotMessageSeverity,
    DotMessageType,
    DotProcessedWorkflowPayload,
    DotWizardInput,
    DotWizardStep,
    DotWorkflowPayload,
    DotEnvironment,
    DotWizardComponentEnum
} from '@dotcms/dotcms-models';

import { DotFormatDateService } from '../dot-format-date/dot-format-date.service';
import { DotGlobalMessageService } from '../dot-global-message/dot-global-message.service';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '../dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from '../dot-iframe/dot-iframe.service';
import { DotMessageDisplayService } from '../dot-message-display/dot-message-display.service';
import { DotMessageService } from '../dot-messages/dot-messages.service';
import { DotWizardService } from '../dot-wizard/dot-wizard.service';
import { DotWorkflowActionsFireService } from '../dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { PushPublishService } from '../push-publish/push-publish.service';

enum DotActionInputs {
    ASSIGNABLE = 'assignable',
    COMMENTABLE = 'commentable',
    COMMENTANDASSIGN = 'commentAndAssign',
    MOVEABLE = 'moveable'
}

interface DotAssignableData {
    roleId: string;
    roleHierarchy: boolean;
}

const EDIT_CONTENT_CALLBACK_FUNCTION = 'saveAssignCallBackAngular';
const VIEW_CONTENT_CALLBACK_FUNCTION = 'angularWorkflowEventCallback';

export const WORKFLOW_STEP_MAP: { [key in DotWizardComponentEnum]: boolean } = {
    commentAndAssign: true,
    pushPublish: true
};
@Injectable()
export class DotWorkflowEventHandlerService {
    private pushPublishService = inject(PushPublishService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotMessageService = inject(DotMessageService);
    private dotWizardService = inject(DotWizardService);
    private dotIframeService = inject(DotIframeService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotFormatDateService = inject(DotFormatDateService);

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
        return inputs.some((input) => input.id === 'pushPublish');
    }

    /**
     * Returns the input needed to collect information of the Workflow inputs.
     *
     * @param {DotCMSWorkflowAction} workflow
     * @param {string} title
     * @returns DotWizardInput
     * @memberof DotWorkflowEventHandlerService
     */
    setWizardInput(workflow: DotCMSWorkflowAction, title: string): DotWizardInput | null {
        const steps: DotWizardStep[] = [];
        this.mergeCommentAndAssign(workflow).forEach((input: DotCMSWorkflowInput) => {
            const formComponentId = input.id as DotWizardComponentEnum;
            if (WORKFLOW_STEP_MAP[formComponentId]) {
                steps.push({
                    component: input.id,
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
        data: DotWorkflowPayload,
        inputs: DotCMSWorkflowInput[]
    ): DotProcessedWorkflowPayload {
        const processedData: DotProcessedWorkflowPayload = { ...data };
        if (this.containsPushPublish(inputs)) {
            processedData['whereToSend'] = data.environment.join();
            processedData['iWantTo'] = data.pushActionSelected;
            processedData['publishTime'] = this.dotFormatDateService.format(
                new Date(data.publishDate),
                'HH-mm'
            );
            processedData['publishDate'] = this.dotFormatDateService.format(
                new Date(data.publishDate),
                'yyyy-MM-dd'
            );
            processedData['expireTime'] = this.dotFormatDateService.format(
                data.expireDate ? new Date(data.expireDate) : new Date(),
                'HH-mm'
            );
            processedData['expireDate'] = this.dotFormatDateService.format(
                data.expireDate ? new Date(data.expireDate) : new Date(),
                'yyyy-MM-dd'
            );
            delete processedData.environment;
            delete processedData.pushActionSelected;
        }

        processedData['contentlet'] = {}; // needed for indexPolicy=WAIT_FOR

        return processedData as DotProcessedWorkflowPayload;
    }

    private mergeCommentAndAssign(workflow: DotCMSWorkflowAction): DotCMSWorkflowInput[] {
        const body: Record<string, boolean> = {};
        let workflows: DotCMSWorkflowInput[];
        workflow.actionInputs.forEach((input) => {
            if (this.isValidActionInput(input.id)) {
                body[input.id] = true;
            }
        });
        if (Object.keys(body).length) {
            workflows = workflow.actionInputs.filter((input) => !this.isValidActionInput(input.id));
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
        const wizardInput = this.setWizardInput(
            event.workflow,
            this.dotMessageService.get('Workflow-Action')
        );
        if (wizardInput) {
            this.dotWizardService
                .open<DotWorkflowPayload>(wizardInput)
                .pipe(take(1))
                .subscribe((data: DotWorkflowPayload) => {
                    this.fireWorkflowAction(event, data);
                });
        }
    }

    private fireWorkflowAction(event: DotCMSWorkflowActionEvent, data?: DotWorkflowPayload): void {
        if (this.isBulkAction(event)) {
            this.dotIframeService.run({ name: 'fireActionLoadingIndicator' });
            this.dotWorkflowActionsFireService
                .bulkFire(this.processBulkData(event, data))
                .pipe(
                    take(1),
                    catchError((error) => {
                        return this.httpErrorManagerService.handle(error);
                    })
                )
                .subscribe((response: DotActionBulkResult | DotHttpErrorHandled) => {
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
                        this.processWorkflowPayload(
                            data as DotWorkflowPayload,
                            event.workflow.actionInputs
                        )
                    ]
                });
            } else {
                this.dotWorkflowActionsFireService
                    .fireTo({
                        inode: event.inode,
                        actionId: event.workflow.id,
                        data: this.processWorkflowPayload(
                            data as DotWorkflowPayload,
                            event.workflow.actionInputs
                        )
                    })
                    .pipe(
                        catchError((error) => {
                            return this.httpErrorManagerService.handle(error);
                        }),
                        take(1)
                    )
                    .subscribe(() => {
                        this.displayNotification(event.workflow.name);
                        if (event.callback === VIEW_CONTENT_CALLBACK_FUNCTION) {
                            this.dotIframeService.run({ name: event.callback });
                        }
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

    private getAssignableData(workflow: DotCMSWorkflowAction): DotAssignableData {
        return {
            roleId: workflow.nextAssign,
            roleHierarchy: workflow.roleHierarchyForAssign
        };
    }

    private isValidActionInput(id: string): boolean {
        return (
            id === DotActionInputs.ASSIGNABLE ||
            id === DotActionInputs.COMMENTABLE ||
            id === DotActionInputs.MOVEABLE
        );
    }

    private processBulkData(
        event: DotCMSWorkflowActionEvent,
        data?: DotWorkflowPayload
    ): DotActionBulkRequestOptions {
        const processedData = this.processWorkflowPayload(
            data as DotWorkflowPayload,
            event.workflow.actionInputs
        );
        const requestOptions: DotActionBulkRequestOptions = {
            workflowActionId: event.workflow.id,
            additionalParams: {
                assignComment: {
                    comment: processedData.comments,
                    assign: processedData.assign
                },
                pushPublish: {
                    whereToSend: processedData.whereToSend,
                    iWantTo: processedData.iWantTo,
                    expireDate: processedData.expireDate,
                    expireTime: processedData.expireTime,
                    publishDate: processedData.publishDate,
                    publishTime: processedData.publishTime,
                    filterKey: processedData.filterKey,
                    timezoneId: processedData.timezoneId
                },
                additionalParamsMap: { _path_to_move: processedData.pathToMove }
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
