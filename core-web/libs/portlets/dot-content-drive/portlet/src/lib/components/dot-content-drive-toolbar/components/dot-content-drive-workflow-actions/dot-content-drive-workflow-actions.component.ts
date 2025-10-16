import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import {
    ActionShowConditions,
    DEFAULT_WORKFLOW_ACTIONS,
    getActionConditions,
    ContentDriveWorkflowAction
} from '../../../../utils/workflow-actions';

@Component({
    selector: 'dot-content-drive-workflow-actions',
    imports: [ButtonModule, DotMessagePipe, NgStyle],
    providers: [DotContentDriveStore, DotWorkflowActionsFireService],
    templateUrl: './dot-content-drive-workflow-actions.component.html',
    styleUrl: './dot-content-drive-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveWorkflowActionsComponent {
    readonly $selectedItems = input.required<DotContentDriveItem[]>({ alias: 'selectedItems' });

    readonly $actionConditions = computed<ActionShowConditions>(() =>
        getActionConditions(this.$selectedItems())
    );

    readonly #store = inject(DotContentDriveStore);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;

    onExecuteDefaultAction(id: string) {
        this.#dotWorkflowActionsFireService
            .fireDefaultAction({
                action: id,
                inodes: this.$selectedItems().map((item) => item.inode)
            })
            .subscribe(() => {
                this.#store.loadItems();
                alert('Workflow action executed');
            });
    }
    /**
     * Determines whether a workflow action should be visible
     * based on the current selection conditions.
     */
    shouldShowAction = (action: ContentDriveWorkflowAction): boolean => {
        const showWhen = action.showWhen;
        const conditions = this.$actionConditions();

        if (!showWhen) return true;

        // Check every defined condition in showWhen.
        // Each must match the corresponding condition in the current state.
        return Object.entries(showWhen).every(([key, expectedValue]) => {
            return conditions[key as keyof ActionShowConditions] === expectedValue;
        });
    };
}
