import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    ActionVisibilityConditions,
    DEFAULT_WORKFLOW_ACTIONS,
    getActionVisibilityConditions
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

    readonly $actionVisibilityConditions = computed<ActionVisibilityConditions>(() =>
        getActionVisibilityConditions(this.$selectedItems())
    );
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;

    onClick(id: string) {
        this.$actionTriggered.emit(id);
    }

    protected shouldHideAction(hideWhen?: ActionVisibilityConditions) {
        const allConditions = this.$actionVisibilityConditions();

        if (hideWhen.emptySelection === allConditions.emptySelection) {
            return true;
        }

        if (hideWhen.contentSelected === allConditions.contentSelected) {
            return true;
        }

        if (hideWhen.multiSelection === allConditions.multiSelection) {
            return true;
        }

        if (hideWhen.archived === allConditions.archived) {
            return true;
        }

        if (hideWhen.lived === allConditions.lived) {
            return true;
        }

        if (hideWhen.working === allConditions.working) {
            return true;
        }

        return false;
    }
}
