import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';

// PrimeNg
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { MenuModule } from 'primeng/menu';
import { OrderListModule } from 'primeng/orderlist';
import { ScrollerModule } from 'primeng/scroller';
import { SkeletonModule } from 'primeng/skeleton';
import { TabViewModule } from 'primeng/tabview';

@NgModule({
    imports: [
        MenuModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule,
        CardModule,
        OrderListModule,
        ListboxModule,
        TabViewModule,
        SkeletonModule,
        ScrollerModule,
        FileUploadModule,
        HttpClientModule
    ],
    exports: [
        MenuModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule,
        CardModule,
        OrderListModule,
        ListboxModule,
        TabViewModule,
        SkeletonModule,
        ScrollerModule,
        FileUploadModule,
        HttpClientModule
    ]
})
export class PrimengModule {}
