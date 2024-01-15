import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    signal
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-workflow-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, SplitButtonModule],
    templateUrl: './dot-workflow-actions.component.html',
    styleUrl: './dot-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWorkflowActionsComponent implements OnChanges {
    @Input({ required: true }) actions: DotCMSWorkflowAction[];
    @Output() actionFired = new EventEmitter<DotCMSWorkflowAction>();

    protected groupedActions = signal<MenuItem[][]>([]);

    ngOnChanges(): void {
        this.groupedActions.set(this.groupActions(this.actions));
    }

    private groupActions(actions: DotCMSWorkflowAction[] = []): MenuItem[][] {
        return actions
            ?.reduce(
                (acc, action) => {
                    if (action?.metadata?.subtype === DotCMSActionSubtype.SEPARATOR) {
                        acc.push([]);
                    } else {
                        acc[acc.length - 1].push({
                            label: action.name,
                            command: () => this.actionFired.emit(action)
                        });
                    }

                    return acc;
                },
                [[]]
            )
            .filter((group) => group.length);
    }
}
