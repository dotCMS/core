import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, take, filter, switchMap, tap } from 'rxjs/operators';

import {
    PushPublishService,
    DotMessageService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotFormatDateService
} from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
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
import { MessageInfo, WorkflowActionResult } from '../../shared/models';

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
    private pushPublishService = inject(PushPublishService);
    private dotMessageService = inject(DotMessageService);
    private messageService = inject(MessageService);
    private dotWizardService = inject(DotWizardService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private dotFormatDateService = inject(DotFormatDateService);

    /**
     * Handle the workflow action, open the wizard and fire the action.
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @param {(callbackName: string, args: unknown[]) => void} [embeddedFunction]
     * @return {*}  {Observable<Partial<WorkflowActionResult>>}
     * @memberof DotEmaWorkflowActionsService
     */
    handleWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        embeddedFunction?: (callbackName: string, args: unknown[]) => void
    ): Observable<Partial<WorkflowActionResult>> {
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
                                map((response: DotActionBulkResult | MessageInfo) => {
                                    if ('summary' in response) {
                                        return response;
                                    }

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
                            map((response) => {
                                if ('summary' in response) {
                                    return response as WorkflowActionResult;
                                }

                                return {
                                    workflowName: event.workflow.name,
                                    callback: event.callback,
                                    args: []
                                };
                            })
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
                        detail: this.dotMessageService.get(
                            'publisher_dialog_environment_mandatory'
                        ),
                        summary: this.dotMessageService.get('Workflow-Action'),
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
            ? {
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
              }
            : { ...data, contentlet: {} };
    }

    /**
     * Open the wizard to collect the data needed to execute the workflow action.
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @return {*}  {Observable<DotWorkflowPayload>}
     * @memberof DotEmaWorkflowActionsService
     */
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

    /**
     * Fire the bulk action
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @param {DotWorkflowPayload} [data]
     * @return {*}  {(Observable<DotActionBulkResult | MessageInfo>)}
     * @memberof DotEmaWorkflowActionsService
     */
    fireBulkWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        data?: DotWorkflowPayload
    ): Observable<DotActionBulkResult | MessageInfo> {
        return this.dotWorkflowActionsFireService.bulkFire(this.processBulkData(event, data)).pipe(
            take(1),
            catchError((error: HttpErrorResponse) => {
                return this.getErrorMessage(error); // These need to be a message with toast.
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

    /**
     * Fire the action
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @param {DotWorkflowPayload} [data]
     * @return {*}  {(Observable<DotCMSContentlet | MessageInfo>)}
     * @memberof DotEmaWorkflowActionsService
     */
    fireWorkflowAction(
        event: DotCMSWorkflowActionEvent,
        data?: DotWorkflowPayload
    ): Observable<DotCMSContentlet | MessageInfo> {
        return this.dotWorkflowActionsFireService
            .fireTo({
                inode: event.inode,
                actionId: event.workflow.id,
                data: this.processWorkflowPayload(data, event.workflow.actionInputs)
            })
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    return this.getErrorMessage(error);
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
     * Check if the action is a bulk action
     *
     * @private
     * @param {DotCMSWorkflowActionEvent} event
     * @return {*}  {boolean}
     * @memberof DotEmaWorkflowActionsService
     */
    private isBulkAction(event: DotCMSWorkflowActionEvent): boolean {
        return !!event.selectedInodes?.length;
    }

    /**
     * Get the assignable data
     *
     * @private
     * @param {DotCMSWorkflowAction} workflow
     * @return {*}  {DotAssignableData}
     * @memberof DotEmaWorkflowActionsService
     */
    private getAssignableData(workflow: DotCMSWorkflowAction): DotAssignableData {
        return {
            roleId: workflow.nextAssign,
            roleHierarchy: workflow.roleHierarchyForAssign
        };
    }

    /**
     * Check if the action input is valid
     *
     * @private
     * @param {string} id
     * @return {*}  {boolean}
     * @memberof DotEmaWorkflowActionsService
     */
    private isValidActionInput(id: string): boolean {
        return (
            id === DotActionInputs.ASSIGNABLE ||
            id === DotActionInputs.COMMENTABLE ||
            id === DotActionInputs.MOVEABLE
        );
    }

    /**
     * Merge the comment and assign action inputs
     *
     * @private
     * @param {DotCMSWorkflowAction} workflow
     * @return {*}  {DotCMSWorkflowInput[]}
     * @memberof DotEmaWorkflowActionsService
     */
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
                body: { ...body, ...this.getAssignableData(workflow) },
                id: DotActionInputs.COMMENTANDASSIGN
            });
        } else {
            return workflow.actionInputs;
        }

        return workflows;
    }

    /**
     * Process the bulk data
     *
     * @private
     * @param {DotCMSWorkflowActionEvent} event
     * @param {DotWorkflowPayload} [data]
     * @return {*}  {DotActionBulkRequestOptions}
     * @memberof DotEmaWorkflowActionsService
     */
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

    /**
     * Get the error message based on the response status
     *
     * @private
     * @param {HttpErrorResponse} [response]
     * @return {*}  {{
     *         detail: string;
     *         summary: string;
     *     }}
     * @memberof DotEmaWorkflowActionsService
     */
    private getErrorMessage(response?: HttpErrorResponse): Observable<MessageInfo> {
        return of(
            {
                [HttpCode.NOT_FOUND]: {
                    summary: this.dotMessageService.get('dot.common.http.error.404.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.404.message')
                },

                [HttpCode.UNAUTHORIZED]: {
                    summary: this.dotMessageService.get('dot.common.http.error.403.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.403.message')
                },

                [HttpCode.FORBIDDEN]: {
                    summary: this.dotMessageService.get('dot.common.http.error.403.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.403.message')
                },
                [HttpCode.SERVER_ERROR]: {
                    summary: this.dotMessageService.get('dot.common.http.error.500.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.500.message')
                },
                [HttpCode.BAD_REQUEST]: {
                    summary: this.dotMessageService.get('dot.common.http.error.400.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.400.message')
                },
                [HttpCode.NO_CONTENT]: {
                    summary: this.dotMessageService.get('dot.common.http.error.204.header'),
                    detail: this.dotMessageService.get('dot.common.http.error.204.message')
                }
            }[response.status]
        );
    }
}
