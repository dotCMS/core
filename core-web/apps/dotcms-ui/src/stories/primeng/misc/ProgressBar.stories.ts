import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ProgressBarModule } from 'primeng/progressbar';
import { ToastModule } from 'primeng/toast';

const meta: Meta = {
    title: 'PrimeNG/Misc/ProgressBar',
    parameters: {
        docs: {
            description: {
                component:
                    'ProgressBar is a process status indicator.: https://primefaces.org/primeng/showcase/#/progressbar'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                ProgressBarModule,
                ToastModule,
                FormsModule
            ]
        })
    ]
};
export default meta;

type Story = StoryObj;

const IndeterminateTemplate = `
  <h3>Indeterminate</h3>
  <p-progressBar mode="indeterminate"></p-progressBar>
`;

export const Indeterminate: Story = {
    parameters: {
        docs: {
            source: {
                code: IndeterminateTemplate
            }
        }
    },
    render: (args) => ({
        props: args,
        template: IndeterminateTemplate
    })
};

const StaticTemplate = `
  <h3>Static</h3>
  <p-progressBar [value]="value" [showValue]="showValue"></p-progressBar>
`;

export const Static: Story = {
    parameters: {
        docs: {
            source: {
                code: StaticTemplate
            }
        }
    },
    args: {
        value: 10,
        showValue: false
    },
    render: (args) => ({
        props: args,
        template: StaticTemplate
    })
};
