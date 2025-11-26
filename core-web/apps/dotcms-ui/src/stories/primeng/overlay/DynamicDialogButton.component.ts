import { Component, OnDestroy, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DynamicDialogProductsComponent } from './DynamicDialogProducts.component';
import { Product } from './Product.interface';

@Component({
    selector: 'dot-p-dialog-button',
    providers: [DialogService, MessageService],
    template: `
        <button
            (click)="show()"
            type="button"
            pButton
            icon="pi pi-info-circle"
            label="Show"></button>
    `
})
export class DynamicDialogButtonComponent implements OnDestroy {
    dialogService = inject(DialogService);
    messageService = inject(MessageService);

    ref: DynamicDialogRef;

    show() {
        this.ref = this.dialogService.open(DynamicDialogProductsComponent, {
            header: 'Choose a Product',
            width: '70%',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000
        });

        this.ref.onClose.subscribe((product: Product) => {
            if (product) {
                this.messageService.add({
                    severity: 'info',
                    summary: 'Product Selected',
                    detail: product.name
                });
            }
        });
    }

    ngOnDestroy() {
        if (this.ref) {
            this.ref.close();
        }
    }
}
