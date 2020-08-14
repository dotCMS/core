import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { pluck } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { DotCMSWorkflowAction, DotCMSWorkflow } from 'dotcms-models';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotCMSWorkflowInput } from '../../../../../projects/dotcms-models/src/dot-workflow-action';

enum DotActionInputs {
    ASSIGNABLE = 'assignable',
    COMMENTABLE = 'commentable',
    COMMENTANDASSIGN = 'commentAndAssign'
}

@Injectable()
export class DotWorkflowsActionsService {
    private workflowStepMap = {
        commentAndAssign: DotCommentAndAssignFormComponent,
        pushPublish: DotPushPublishFormComponent
    };

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Return a list of actions based on the workflows received
     *
     * @param {DotCMSWorkflow[]} [workflows=[]]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByWorkflows(workflows: DotCMSWorkflow[] = []): Observable<DotCMSWorkflowAction[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Post,
                url: '/api/v1/workflow/schemes/actions/NEW',
                body: {
                    schemes: workflows.map(this.getWorkFlowId)
                }
            })
            .pipe(pluck('entity'));
    }

    /**
     * Returns the workflow actions of the passed inode
     *
     * @param {string} inode
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByInode(inode: string): Observable<DotCMSWorkflowAction[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/workflow/contentlet/${inode}/actions`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Returns the steps needed to collect information of the Workflow inputs.
     *
     * @param {DotCMSWorkflowAction} workflow
     * @returns DotWizardStep[]
     * @memberof DotWorkflowsActionsService
     */
    setWizardSteps(workflow: DotCMSWorkflowAction): DotWizardStep<any>[] {
        const steps: DotWizardStep<any>[] = [];
        this.mergeCommentAndAssign(workflow).forEach((input: DotCMSWorkflowInput) => {
            if (this.workflowStepMap[input.id]) {
                steps.push({
                    component: this.workflowStepMap[input.id],
                    data: input.body
                });
            }
        });
        return steps;
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

    private isCommentOrAssign(id: string): boolean {
        return id === DotActionInputs.ASSIGNABLE || id === DotActionInputs.COMMENTABLE;
    }

    private getWorkFlowId(workflow: DotCMSWorkflow): string {
        return workflow && workflow.id;
    }

    private getAssignableData(workflow: DotCMSWorkflowAction): { [key: string]: string } {
        return { roleId: workflow.nextAssign };
    }
}
