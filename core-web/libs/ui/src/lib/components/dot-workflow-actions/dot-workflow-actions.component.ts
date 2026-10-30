import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    input,
    OnChanges,
    output,
    signal
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type ButtonSize = 'normal' | 'small' | 'large';

const InplaceButtonSizePrimeNg: Record<ButtonSize, string> = {
    normal: '', // default
    small: 'p-button-sm',
    large: 'p-button-lg'
};

interface WorkflowActionsGroup {
    mainAction: MenuItem;
    subActions: MenuItem[];
}

@Component({
    selector: 'dot-workflow-actions',
    imports: [CommonModule, ButtonModule, SplitButtonModule, DotMessagePipe],
    templateUrl: './dot-workflow-actions.component.html',
    styleUrl: './dot-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWorkflowActionsComponent implements OnChanges {
    /**
     * List of actions to display
     *
     * @type {DotCMSWorkflowAction[]}
     * @memberof DotWorkflowActionsComponent
     */
    actions = input.required<DotCMSWorkflowAction[]>();
    /**
     * Show a loading button spinner
     *
     * @memberof DotWorkflowActionsComponent
     */
    loading = input<boolean>(false);
    /**
     * Disable the actions
     *
     * @memberof DotWorkflowActionsComponent
     */
    disabled = input<boolean>(false);
    /**
     * Group the actions by separator
     *
     * @memberof DotWorkflowActionsComponent
     */
    groupActions = input<boolean>(false);
    /**
     * Button size
     *
     * @type {ButtonSize}
     * @memberof DotWorkflowActionsComponent
     */
    size = input<ButtonSize>('normal');
    /**
     * Emits when an action is selected
     *
     * @memberof DotWorkflowActionsComponent
     */
    actionFired = output<DotCMSWorkflowAction>();

    protected $groupedActions = signal<WorkflowActionsGroup[]>([]);
    protected sizeClass!: string;

    ngOnChanges(): void {
        this.sizeClass = InplaceButtonSizePrimeNg[this.size()];

        if (!this.actions().length) {
            this.$groupedActions.set([]);

            return;
        }

        this.setActions();
    }

    /**
     * Set the actions to display
     *
     * @private
     * @memberof DotWorkflowActionsComponent
     */
    private setActions(): void {
        const groups = this.createGroups(this.actions());
        const actions = groups.map((group) => {
            const [first, ...rest] = group;
            const mainAction = this.createActions(first);
            const subActions = rest.map((action) => this.createActions(action));

            return { mainAction, subActions };
        });

        this.$groupedActions.set(actions);
    }

    /**
     * Create the groups of actions
     * Each group is create when a separator is found
     *
     * @private
     * @param {DotCMSWorkflowAction[]} actions
     * @return {*}  {DotCMSWorkflowAction[][]}
     * @memberof DotWorkflowActionsComponent
     */
    private createGroups(actions: DotCMSWorkflowAction[]): DotCMSWorkflowAction[][] {
        if (!this.groupActions()) {
            // Remove the separator from the actions and return the actions grouped
            const formatActions = actions.filter(
                (action) => action?.metadata?.subtype !== DotCMSActionSubtype.SEPARATOR
            );

            return [formatActions].filter((group) => !!group.length);
        }

        // Create a new group every time we find a separator
        return actions
            .reduce<DotCMSWorkflowAction[][]>(
                (acc, action) => {
                    if (action?.metadata?.subtype === DotCMSActionSubtype.SEPARATOR) {
                        acc.push([]);
                    } else {
                        acc[acc.length - 1].push(action);
                    }

                    return acc;
                },
                [[]]
            )
            .filter((group) => !!group.length);
    }

    /**
     * Create the action menu item
     *
     * @private
     * @param {DotCMSWorkflowAction} action
     * @return {*}  {MenuItem}
     * @memberof DotWorkflowActionsComponent
     */
    private createActions(action: DotCMSWorkflowAction): MenuItem {
        return {
            label: action.name,
            command: () => this.actionFired.emit(action)
        };
    }
}
