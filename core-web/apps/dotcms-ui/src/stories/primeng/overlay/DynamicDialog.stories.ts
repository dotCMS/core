// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular';

import { HttpClientModule } from '@angular/common/http';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { DynamicDialogButtonComponent } from './DynamicDialogButton.component';
import { DynamicDialogProductsComponent } from './DynamicDialogProducts.component';
import { ProductService } from './SharedProducts.service';

export default {
    title: 'PrimeNG/Overlay/DynamicDialog',
    component: DynamicDialogButtonComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'Dialogs can be created dynamically with any component as the content using a DialogService: https://primefaces.org/primeng/showcase/#/dynamicdialog'
            }
        }
    }
} as Meta;

export const Basic = () => ({
    component: DynamicDialogButtonComponent,
    moduleMetadata: {
        imports: [
            BrowserModule,
            BrowserAnimationsModule,
            DynamicDialogModule,
            ToastModule,
            TableModule,
            ButtonModule,
            HttpClientModule
        ],
        providers: [ProductService, DynamicDialogRef, DynamicDialogConfig],
        declarations: [DynamicDialogProductsComponent],
        entryComponents: [DynamicDialogProductsComponent]
    }
});

Basic.parameters = {
    docs: {
        source: {
            code: `<button type="button" (click)="show()" pButton icon="pi pi-info-circle" label="Show"></button>`
        },
        iframeHeight: 300
    }
};
