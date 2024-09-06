import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

import { RippleComponent } from './Ripple.component';

const meta: Meta = {
    title: 'PrimeNG/Misc/Ripple',
    component: RippleComponent,
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
};
export default meta;

type Story = StoryObj;

export const Default: Story = {};
