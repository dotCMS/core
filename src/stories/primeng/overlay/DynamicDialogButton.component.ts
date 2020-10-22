import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { DynamicDialogProductsComponent } from './DynamicDialogProducts.component';
import { Product } from './Product.interface';
import { DialogService } from 'primeng/dynamicdialog';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

@Component({
    selector: 'app-p-dialog-button',
    providers: [DialogService, MessageService],
    template: `<button
        type="button"
        (click)="show()"
        pButton
        icon="pi pi-info-circle"
        label="Show"
    ></button>`
})
export class DynamicDialogButtonComponent {
    constructor(public dialogService: DialogService, public messageService: MessageService) {}

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
