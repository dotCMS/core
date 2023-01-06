import { NgModule } from '@angular/core';

// PrimeNg
import { MenuModule } from 'primeng/menu';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { OrderListModule } from 'primeng/orderlist';
import { ListboxModule } from 'primeng/listbox';
import { TabViewModule } from 'primeng/tabview';
import { SkeletonModule } from 'primeng/skeleton';
import { ScrollerModule } from 'primeng/scroller';

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
        ScrollerModule
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
        ScrollerModule
    ]
})
export class PrimengModule {}
