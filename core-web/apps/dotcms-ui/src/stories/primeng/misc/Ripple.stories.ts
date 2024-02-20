import { Meta, moduleMetadata } from '@storybook/angular';

import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

import { RippleComponent } from './Ripple.component';

export default {
    title: 'PrimeNG/Misc/Ripple',
    parameters: {
        docs: {
            description: {
                component:
                    'Ripple directive adds ripple effect to the host element.: https://primefaces.org/primeng/showcase/#/ripple'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [RippleModule, ButtonModule, BrowserModule, BrowserAnimationsModule]
        })
    ]
} as Meta;

export const Default = () => ({
    component: RippleComponent,
    props: {}
});
