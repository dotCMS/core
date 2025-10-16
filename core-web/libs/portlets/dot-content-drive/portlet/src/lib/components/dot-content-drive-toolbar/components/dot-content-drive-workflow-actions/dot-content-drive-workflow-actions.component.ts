import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    ActionShowConditions,
    DEFAULT_WORKFLOW_ACTIONS,
    getActionConditions,
    ContentDriveWorkflowAction
} from '../../../../utils/workflow-actions';

@Component({
    selector: 'dot-content-drive-workflow-actions',
    imports: [ButtonModule, DotMessagePipe, NgStyle],
    templateUrl: './dot-content-drive-workflow-actions.component.html',
    styleUrl: './dot-content-drive-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveWorkflowActionsComponent {
    readonly $selectedItems = input.required<DotContentDriveItem[]>({ alias: 'selectedItems' });
    readonly $actionTriggered = output<string>({ alias: 'actionTriggered' });

    readonly $actionConditions = computed<ActionShowConditions>(() =>
        getActionConditions(this.$selectedItems())
    );
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;

    onClick(id: string) {
        this.$actionTriggered.emit(id);
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
