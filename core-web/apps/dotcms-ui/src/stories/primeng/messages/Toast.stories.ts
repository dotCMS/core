import {
    Meta,
    StoryObj,
    applicationConfig,
    moduleMetadata,
    componentWrapperDecorator
} from '@storybook/angular';

import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';

import { ToastComponent } from './Toast.component';

const meta: Meta<ToastComponent> = {
    title: 'PrimeNG/Messages/Toast',
    component: ToastComponent,
    args: {
        severity: 'success',
        summary: 'Success Message',
        detail: 'The action "Publish" was executed succesfully',
        position: 'top-right',
        life: 2000,
        icon: 'pi-check-circle'
    },
    argTypes: {
        severity: {
            options: ['success', 'info', 'error', 'warn'],
            control: { type: 'select' },
            description: 'Severity level of the message'
        },
        summary: {
            control: { type: 'text' },
            description: 'Summary text of the message'
        },
        detail: {
            control: { type: 'text' },
            description: 'Detail text of the message'
        },
        position: {
            options: [
                'top-right',
                'top-left',
                'bottom-left',
                'bottom-right',
                'top-center',
                'bottom-center',
                'center'
            ],
            control: { type: 'select' },
            description: 'Position of the message'
        },
        life: {
            control: { type: 'number' },
            description: 'Delay in milliseconds to close the message'
        },
        icon: {
            control: { type: 'text' },
            description:
                'Icon of the message check https://primeng.org/icons for icons. You can hide it by setting it to "hidden"'
        }
    },
    parameters: {
        docs: {
            description: {
                component:
                    'Tooltip directive provides advisory information for a component.: https://www.primefaces.org/primeng-v15-lts/toast'
            }
        }
    },
    decorators: [
        moduleMetadata({
            providers: [MessageService]
        }),
        // Apply application config to all stories
        applicationConfig({
            // List of providers and environment providers that should be available to the root component and all its children.
            providers: [MessageService, importProvidersFrom(BrowserAnimationsModule)]
        }),
        componentWrapperDecorator((story) => `<div class="w-full h-20rem">${story}</div>`)
    ]
};
export default meta;

type Story = StoryObj;

export const Base: Story = {};
