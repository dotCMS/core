import { Meta, StoryFn, applicationConfig, moduleMetadata } from '@storybook/angular';

import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

import { ToastComponent } from './Toast.component';

export default {
    title: 'PrimeNG/Messages/Toast',
    component: ToastComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'Tooltip directive provides advisory information for a component.: https://primefaces.org/primeng/showcase/#/tooltip'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ToastModule, ButtonModule],
            providers: [MessageService]
        }),
        // Apply application config to all stories
        applicationConfig({
            // List of providers and environment providers that should be available to the root component and all its children.
            providers: [MessageService, importProvidersFrom(BrowserAnimationsModule)]
        })
    ]
} as Meta;

const Template = (): StoryFn => (args) => ({
    props: args
});

export const Base = Template();
