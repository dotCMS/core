import { ChangeDetectionStrategy, Component, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_WORKFLOW_ACTIONS } from '../../../../utils/workflow-actions';

@Component({
    selector: 'dot-content-drive-workflow-actions',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-content-drive-workflow-actions.component.html',
    styleUrl: './dot-content-drive-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveWorkflowActionsComponent {
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;
    readonly $actionTriggered = output<string>({ alias: 'actionTriggered' });

    onClick(id: string) {
        this.$actionTriggered.emit(id);
        // console.log(id);
    }
}
