import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { DotMessagePipe, DotMessagePipeModule } from '@dotcms/ui';

@Component({
    selector: 'dotcms-remove-confirm-dialog',
    templateUrl: './remove-confirm-dialog.component.html',
    styleUrls: ['./remove-confirm-dialog.component.scss'],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ConfirmPopupModule, ButtonModule, DotMessagePipeModule],
    providers: [ConfirmationService, DotMessagePipe]
})
export class RemoveConfirmDialogComponent {
    @Output() deleteConfirmed: EventEmitter<void> = new EventEmitter();
    @Output() deleteRejected: EventEmitter<void> = new EventEmitter();
    constructor(
        private confirmationService: ConfirmationService,
        private dotMessagePipe: DotMessagePipe
    ) {}

    openConfirmationDialog(event: Event): void {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessagePipe.transform(
                'dot.template.builder.comfirmation.popup.message'
            ),
            icon: 'pi pi-info-circle',
            accept: () => {
                this.deleteConfirmed.emit();
            },
            reject: () => {
                this.deleteRejected.emit();
            }
        });
    }
}
