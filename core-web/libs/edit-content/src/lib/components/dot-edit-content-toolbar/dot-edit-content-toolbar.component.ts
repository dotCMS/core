import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-toolbar',
    standalone: true,
    imports: [CommonModule, ToolbarModule, SplitButtonModule, ButtonModule],
    templateUrl: './dot-edit-content-toolbar.component.html',
    styleUrls: ['./dot-edit-content-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentToolbarComponent implements OnChanges {
    @Input() actions: DotCMSWorkflowAction[];
    @Output() actionFired = new EventEmitter<DotCMSWorkflowAction>();

    private _grouppedActions: MenuItem[][] = [];

    get groupedActions(): MenuItem[][] {
        return this._grouppedActions;
    }

    ngOnChanges(): void {
        this._grouppedActions = this.groupActions(this.actions);
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
