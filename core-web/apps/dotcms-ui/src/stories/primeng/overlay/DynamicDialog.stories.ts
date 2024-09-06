import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

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

const meta: Meta<DynamicDialogButtonComponent> = {
    title: 'PrimeNG/Overlay/DynamicDialog',
    component: DynamicDialogButtonComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'Dialogs can be created dynamically with any component as the content using a DialogService: https://primefaces.org/primeng/showcase/#/dynamicdialog'
            },
            source: {
                code: `<button type="button" (click)="show()" pButton icon="pi pi-info-circle" label="Show"></button>`
            },
            iframeHeight: 300
        }
    },
    decorators: [
        moduleMetadata({
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
        })
    ]
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
