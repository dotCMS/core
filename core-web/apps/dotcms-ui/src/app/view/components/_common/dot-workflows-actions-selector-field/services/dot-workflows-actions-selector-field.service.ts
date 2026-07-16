import { BehaviorSubject, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { SelectItem, SelectItemGroup } from 'primeng/api';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSWorkflow, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

@Injectable()
export class DotWorkflowsActionsSelectorFieldService {
    private dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    private data$: BehaviorSubject<SelectItemGroup[]> = new BehaviorSubject([]);

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
