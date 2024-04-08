import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, take, filter, switchMap, tap } from 'rxjs/operators';

import {
    PushPublishService,
    DotMessageService,
    DotWizardService,
    DotHttpErrorManagerService,
    DotWorkflowActionsFireService,
    DotFormatDateService,
    DotHttpErrorHandled
} from '@dotcms/data-access';
import {
    DotActionBulkRequestOptions,
    DotActionBulkResult,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotCMSWorkflowInput,
    DotProcessedWorkflowPayload,
    DotWizardInput,
    DotWizardStep,
    DotWorkflowPayload,
    DotEnvironment,
    DotWizardComponentEnum,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

import { EDIT_CONTENT_CALLBACK_FUNCTION } from '../../shared/consts';

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

export const WORKFLOW_STEP_MAP: { [key in DotWizardComponentEnum]: boolean } = {
    commentAndAssign: true,
    pushPublish: true
};

@Injectable()
export class DotEmaWorkflowActionsService {
    constructor(
        private pushPublishService: PushPublishService,
        private dotMessageService: DotMessageService,
        private messageService: MessageService,
        private dotWizardService: DotWizardService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotFormatDateService: DotFormatDateService
    ) {}

    handleWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        embeddedFunction?: (callbackName: string, args: unknown[]) => void
    ): Observable<{
        workflowName: string;
        callback: string;
        args: unknown[];
    }> {
        return this.triggerWizardOpening(event).pipe(
            take(1),
            switchMap((event) =>
                this.openWizard(event).pipe(
                    tap(() => {
                        this.messageService.add({
                            life: 1000,
                            detail: this.dotMessageService.get(
                                'edit.ema.page.executing.workflow.action'
                            ),
                            summary: this.dotMessageService.get('Workflow-Action'),
                            severity: 'info'
                        });
                    }),
                    switchMap((payload) => {
                        if (this.isBulkAction(event)) {
                            return this.fireBulkWorkflowAction(event, payload).pipe(
                                tap(() => {
                                    embeddedFunction('fireActionLoadingIndicator', []);
                                }),
                                map((response: DotActionBulkResult | DotHttpErrorHandled) => {
                                    return {
                                        workflowName: event.workflow.name,
                                        callback: event.callback,
                                        args: [response]
                                    };
                                })
                            );
                        } else if (event.callback === EDIT_CONTENT_CALLBACK_FUNCTION) {
                            return of({
                                workflowName: event.workflow.name,
                                callback: event.callback,
                                args: [
                                    event.workflow.id,
                                    this.processWorkflowPayload(
                                        payload,
                                        event.workflow.actionInputs
                                    )
                                ]
                            });
                        }

                        return this.fireWorkflowAction(event, payload).pipe(
                            map(() => ({
                                workflowName: event.workflow.name,
                                callback: event.callback,
                                args: []
                            }))
                        );
                    })
                )
            )
        );
    }

    /**
     * Fire the event to open the wizard to collect data
     * @param {DotCMSWorkflowActionEvent} event
     * @memberof DotWorkflowEventHandlerService
     */
    triggerWizardOpening(event: DotCMSWorkflowActionEvent): Observable<DotCMSWorkflowActionEvent> {
        return this.containsPushPublish(event.workflow.actionInputs)
            ? this.checkPublishEnvironments().pipe(
                  filter((hasEnviroments: boolean) => hasEnviroments),
                  take(1),
                  map(() => event)
              )
            : of(event);
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
                if (!environments.length) {
                    this.messageService.add({
                        life: 3000,
                        summary: this.dotMessageService.get(
                            'editpage.actions.fire.error.add.environment'
                        ),
                        severity: 'error'
                    });
                }

                return !!environments.length;
            })
        );
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
        return this.containsPushPublish(inputs)
            ? { ...data, contentlet: {} }
            : {
                  ...data,
                  whereToSend: data.environment.join(),
                  iWantTo: data.pushActionSelected,
                  publishTime: this.dotFormatDateService.format(
                      new Date(data.publishDate),
                      'HH-mm'
                  ),
                  publishDate: this.dotFormatDateService.format(
                      new Date(data.publishDate),
                      'yyyy-MM-dd'
                  ),
                  expireTime: this.dotFormatDateService.format(
                      data.expireDate ? new Date(data.expireDate) : new Date(),
                      'HH-mm'
                  ),
                  expireDate: this.dotFormatDateService.format(
                      data.expireDate ? new Date(data.expireDate) : new Date(),
                      'yyyy-MM-dd'
                  ),
                  environment: undefined,
                  pushActionSelected: undefined,
                  contentlet: {} // needed for indexPolicy=WAIT_FOR
              };
    }
    openWizard(event: DotCMSWorkflowActionEvent): Observable<DotWorkflowPayload> {
        const wizardInput = this.setWizardInput(
            event.workflow,
            this.dotMessageService.get('Workflow-Action')
        );

        return this.dotWizardService.open<DotWorkflowPayload>(wizardInput).pipe(
            filter(() => !!wizardInput),
            take(1)
        );
    }

    fireBulkWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        data?: DotWorkflowPayload
    ): Observable<DotActionBulkResult | DotHttpErrorHandled> {
        return this.dotWorkflowActionsFireService.bulkFire(this.processBulkData(event, data)).pipe(
            take(1),
            catchError((error) => {
                return this.httpErrorManagerService.handle(error); // These need to be a message with toast.
            })
        );
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
        const steps = this.mergeCommentAndAssign(workflow).reduce((steps, input) => {
            const formComponentId = input.id as DotWizardComponentEnum;
            if (WORKFLOW_STEP_MAP[formComponentId]) {
                steps.push({
                    component: input.id,
                    data: input.body
                });
            }

            return steps;
        }, [] as DotWizardStep[]);

        return steps.length
            ? {
                  title,
                  steps
              }
            : null;
    }

    fireWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        data?: DotWorkflowPayload
    ): Observable<DotCMSContentlet | DotHttpErrorHandled> {
        return this.dotWorkflowActionsFireService
            .fireTo({
                inode: event.inode,
                actionId: event.workflow.id,
                data: this.processWorkflowPayload(data, event.workflow.actionInputs)
            })
            .pipe(
                catchError((error) => {
                    return this.httpErrorManagerService.handle(error);
                }),
                take(1)
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

    private isBulkAction(event: DotCMSWorkflowActionEvent): boolean {
        return !!event.selectedInodes?.length;
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
        const processedData = this.processWorkflowPayload(data, event.workflow.actionInputs);
        const requestOptions: DotActionBulkRequestOptions = {
            workflowActionId: event.workflow.id,
            additionalParams: {
                additionalParamsMap: { _path_to_move: processedData.pathToMove },
                assignComment: {
                    assign: processedData.assign,
                    comment: processedData.comments
                },
                pushPublish: {
                    whereToSend: processedData.whereToSend,
                    publishTime: processedData.publishTime,
                    expireDate: processedData.expireDate,
                    publishDate: processedData.publishDate,
                    expireTime: processedData.expireTime,
                    filterKey: processedData.filterKey,
                    timezoneId: processedData.timezoneId,
                    iWantTo: processedData.iWantTo
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
