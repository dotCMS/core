import { Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DeferComponent } from './Defer.component';
import { BrowserModule } from '@angular/platform-browser';
import { DeferModule } from 'primeng/defer';
import { TableModule } from 'primeng/table';
import { HttpClientModule } from '@angular/common/http';

export default {
    title: 'PrimeNG/Misc/Defer',
    parameters: {
        docs: {
            description: {
                component:
                    'Defer postpones the loading the content that is initially not in the viewport until it becomes visible on scroll.: https://primefaces.org/primeng/showcase/#/defer'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                DeferModule,
                ToastModule,
                TableModule,
                HttpClientModule
            ],
            providers: [MessageService]
        })
    ]
} as Meta;

// inbox screen default state
export const Default = () => ({
    component: DeferComponent,
    props: {
        cars: [
            { brand: 'VW', year: 2012, color: 'Orange', vin: 'dsad231ff' },
            { brand: 'Audi', year: 2011, color: 'Black', vin: 'gwregre345' },
            { brand: 'Renault', year: 2005, color: 'Gray', vin: 'h354htr' },
            { brand: 'BMW', year: 2003, color: 'Blue', vin: 'j6w54qgh' },
            {
                brand: 'Mercedes',
                year: 1995,
                color: 'Orange',
                vin: 'hrtwy34'
            },
            { brand: 'Volvo', year: 2005, color: 'Black', vin: 'jejtyj' },
            { brand: 'Honda', year: 2012, color: 'Yellow', vin: 'g43gr' },
            { brand: 'Jaguar', year: 2013, color: 'Orange', vin: 'greg34' },
            { brand: 'Ford', year: 2000, color: 'Black', vin: 'h54hw5' },
            { brand: 'Fiat', year: 2013, color: 'Red', vin: '245t2s' }
        ]
    }
});
