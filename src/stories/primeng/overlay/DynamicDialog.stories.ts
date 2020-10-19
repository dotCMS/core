// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { HttpClientModule } from '@angular/common/http';
import { ProductService } from './SharedProducts.service';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import {
  DynamicDialogButtonComponent,
} from './DynamicDialogButton.component';
import { DynamicDialogProductsComponent } from './DynamicDialogProducts.component';

export default {
  title: 'PrimeNG/Overlay/DynamicDialog',
  component: DynamicDialogButtonComponent,
  parameters: {
    docs: {
      description: {
        component:
          'Dialogs can be created dynamically with any component as the content using a DialogService: https://primefaces.org/primeng/showcase/#/dynamicdialog',
      },
    },
  },
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
  },
});

Basic.parameters = {
  docs: {
    source: {
      code: `<button type="button" (click)="show()" pButton icon="pi pi-info-circle" label="Show"></button>`,
    },
    iframeHeight: 300,
  },
};