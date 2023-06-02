import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
    selector: 'dotcms-remove-row',
    templateUrl: './remove-row.component.html',
    styleUrls: ['./remove-row.component.scss'],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ConfirmDialogModule, ButtonModule, BrowserAnimationsModule],
    providers: [ConfirmationService]
})
export class RemoveRowComponent {
    @Output() deleteRow: EventEmitter<void> = new EventEmitter();
    constructor(private confirmationService: ConfirmationService) {}

    confirm(): void {
        this.confirmationService.confirm({
            message: 'Are you sure you want to proceed deleting this item?',
            icon: 'pi pi-info-circle',
            acceptLabel: 'Yes',
            rejectLabel: 'No',
            rejectButtonStyleClass: 'p-button-sm p-button-secondary',
            acceptButtonStyleClass: 'p-button-sm p-button-primary'
        });
    }
}
