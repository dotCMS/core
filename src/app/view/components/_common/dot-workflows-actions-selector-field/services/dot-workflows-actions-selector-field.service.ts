import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, take, catchError } from 'rxjs/operators';

import { SelectItemGroup, SelectItem } from 'primeng/primeng';

import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';
import { DotCMSWorkflowAction, DotCMSWorkflow } from 'dotcms-models';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable()
export class DotWorkflowsActionsSelectorFieldService {
    private data$: BehaviorSubject<SelectItemGroup[]> = new BehaviorSubject([]);

    constructor(
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    /**
     * Get actions grouped by workflows
     *
     * @returns {Observable<SelectItemGroup[]>}
     * @memberof DotWorkflowsActionsSelectorFieldService
     */
    get(): Observable<SelectItemGroup[]> {
        return this.data$;
    }

    /**
     * Update the actions with workflows passed
     *
     * @param {DotCMSWorkflow[]} workflows
     * @memberof DotWorkflowsActionsSelectorFieldService
     */
    load(workflows: DotCMSWorkflow[]): void {
        if (workflows && workflows.length) {
            this.dotWorkflowsActionsService
                .getByWorkflows(workflows)
                .pipe(
                    take(1),
                    map((actions: DotCMSWorkflowAction[]) =>
                        this.getSelectItemGroups(workflows, actions)
                    ),
                    catchError((err: HttpErrorResponse) =>
                        this.dotHttpErrorManagerService.handle(err).pipe(map(() => []))
                    )
                )
                .subscribe((actions: SelectItemGroup[]) => {
                    this.data$.next(actions);
                });
        } else {
            this.data$.next([]);
        }
    }

    private getSelectItemGroups(
        workflows: DotCMSWorkflow[],
        actions: DotCMSWorkflowAction[]
    ): SelectItemGroup[] {
        return workflows.map((workflow: DotCMSWorkflow) => {
            const { label, value } = this.getSelectItem(workflow);

            return {
                label,
                value,
                items: this.getActionsByWorkflowId(workflow, actions).map(this.getSelectItem)
            };
        });
    }

    private getSelectItem({ name, id }: DotCMSWorkflowAction | DotCMSWorkflow): SelectItem {
        return {
            label: name,
            value: id
        };
    }

    private getActionsByWorkflowId(
        { id }: DotCMSWorkflow,
        actions: DotCMSWorkflowAction[]
    ): DotCMSWorkflowAction[] {
        return actions.filter((action: DotCMSWorkflowAction) => action.schemeId === id);
    }
}
