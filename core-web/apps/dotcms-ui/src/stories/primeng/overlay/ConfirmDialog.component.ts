import { Component, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
    selector: 'dot-p-confirm-dialog',
    standalone: true,
    imports: [ConfirmDialogModule, ButtonModule],
    template: `
        <p-confirmDialog [style]="{ width: '400px' }" [baseZIndex]="10000">
            <p-footer>
                <button
                    class="p-button-secondary"
                    type="button"
                    pButton
                    icon="pi pi-times"
                    label="Cancel"></button>
                <button type="button" pButton icon="pi pi-check" label="Delete"></button>
            </p-footer>
        </p-confirmDialog>
        <button type="text" (click)="confirm()" pButton icon="pi pi-check" label="Confirm"></button>
    `
})
export class ConfirmDialogComponent {
    private confirmationService = inject(ConfirmationService);

    confirm(): void {
        this.confirmationService.confirm({
            message:
                'Are you sure you want to delete the Content Type Contact and all the content associated with it? (This operation can not be undone)',
            header: 'Delete Content Type',
            acceptLabel: 'Close',
            rejectLabel: 'Cancel',
            rejectButtonStyleClass: 'p-button-secondary'
        });
    }
}
