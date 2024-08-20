import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ProgressSpinnerModule } from 'primeng/progressspinner';

const CustomTemplate = `
  <p-progressSpinner [style]="{width: '50px', height: '50px'}" styleClass="custom-spinner" strokeWidth="8" fill="#EEEEEE" animationDuration=".5s"></p-progressSpinner>
`;

const meta: Meta = {
    title: 'PrimeNG/Misc/ProgressSpinner',
    parameters: {
        docs: {
            description: {
                component:
                    'ProgressSpinner is a process status indicator.: https://primefaces.org/primeng/showcase/#/progressspinner'
            },
            source: {
                code: CustomTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [BrowserModule, BrowserAnimationsModule, ProgressSpinnerModule, FormsModule]
        })
    ],
    args: {
        value: 10,
        showValue: false
    },
    render: (args) => ({
        props: args,
        template: CustomTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Custom: Story = {};
