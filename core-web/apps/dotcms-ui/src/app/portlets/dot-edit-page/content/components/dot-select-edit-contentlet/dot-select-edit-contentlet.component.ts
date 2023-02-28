import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

enum SELECT_OPTIONS {
    CURRENT = 'CURRENT',
    ALL = 'ALL'
}

@Component({
    selector: 'dot-select-edit-contentlet',
    standalone: true,
    imports: [CommonModule, DotDialogModule],
    templateUrl: './dot-select-edit-contentlet.component.html',
    styleUrls: ['./dot-select-edit-contentlet.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectEditContentletComponent {
    @Input()
    contentlet: unknown;

    @Input() show = false;

    @Output()
    data = new EventEmitter<unknown>();

    @Output()
    hideDialog = new EventEmitter<void>();

    public selected = SELECT_OPTIONS.CURRENT;
    public readonly options = SELECT_OPTIONS;

    dialogActions = {
        accept: {
            label: 'Accept',
            disabled: false,
            action: () => this.globalEditAction.bind(this)
        },
        cancel: {
            label: 'Cancel',
            disabled: false,
            action: this.hideDialog.emit()
        }
    };

    constructor() {
        //
    }

    changeSelection(selected: SELECT_OPTIONS) {
        this.selected = selected;

        this.dialogActions = {
            ...this.dialogActions,
            accept: {
                ...this.dialogActions.accept,
                action:
                    selected === SELECT_OPTIONS.ALL
                        ? this.copyAndEditAction.bind(this)
                        : this.globalEditAction.bind(this)
            }
        };
    }

    private copyAndEditAction(): void {
        this.data.emit(this.contentlet);
    }

    private globalEditAction(): void {
        this.data.emit(this.contentlet);
    }
}
