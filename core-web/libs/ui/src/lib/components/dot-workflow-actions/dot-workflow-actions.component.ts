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

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-workflow-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, SplitButtonModule, DotMessagePipe],
    templateUrl: './dot-workflow-actions.component.html',
    styleUrl: './dot-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWorkflowActionsComponent implements OnChanges {
    @Input({ required: true }) actions: DotCMSWorkflowAction[];
    @Input() loading: boolean = false;
    @Input() groupAction: boolean = false;
    @Output() actionFired = new EventEmitter<DotCMSWorkflowAction>();

    protected groupedActions = signal<MenuItem[][]>([]);

    ngOnChanges(): void {
        this.groupedActions.set(
            this.groupAction ? this.groupActions(this.actions) : this.formatActions(this.actions)
        );
    }

    /**
     * Group actions by separator
     *
     * @private
     * @param {DotCMSWorkflowAction[]} [actions=[]]
     * @return {*}  {MenuItem[][]}
     * @memberof DotWorkflowActionsComponent
     */
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

    /**
     * Remove the separator from the actions and return the actions grouped
     * in a single group.
     *
     * @private
     * @param {DotCMSWorkflowAction[]} [actions=[]]
     * @return {*}  {MenuItem[][]}
     * @memberof DotWorkflowActionsComponent
     */
    private formatActions(actions: DotCMSWorkflowAction[] = []): MenuItem[][] {
        const formatedActions = actions?.reduce((acc, action) => {
            if (action?.metadata?.subtype !== DotCMSActionSubtype.SEPARATOR) {
                acc.push({
                    label: action.name,
                    command: () => this.actionFired.emit(action)
                });
            }

            return acc;
        }, []);

        return [formatedActions].filter((group) => group.length);
    }
}
