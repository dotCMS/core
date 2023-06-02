import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

@Component({
    selector: 'dotcms-remove-confirm-dialog',
    templateUrl: './remove-confirm-dialog.component.html',
    styleUrls: ['./remove-confirm-dialog.component.scss'],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ConfirmPopupModule, ButtonModule],
    providers: [ConfirmationService]
})
export class RemoveConfirmDialogComponent {
    @Output() confirmEvent: EventEmitter<void> = new EventEmitter();
    constructor(private confirmationService: ConfirmationService) {}

    confirm(event: Event): void {
        this.confirmationService.confirm({
            target: event.target,
            message: 'Are you sure you want to proceed deleting this item?',
            icon: 'pi pi-info-circle',
            accept: () => {
                this.confirmEvent.emit();
            }
        });
    }
}
