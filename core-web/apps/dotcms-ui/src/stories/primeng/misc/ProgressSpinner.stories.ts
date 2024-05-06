import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ProgressSpinnerModule } from 'primeng/progressspinner';

export default {
    title: 'PrimeNG/Misc/ProgressSpinner',
    parameters: {
        docs: {
            description: {
                component:
                    'ProgressSpinner is a process status indicator.: https://primefaces.org/primeng/showcase/#/progressspinner'
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
    }
} as Meta;

const CustomTemplate = `
  <p-progressSpinner [style]="{width: '50px', height: '50px'}" styleClass="custom-spinner" strokeWidth="8" fill="#EEEEEE" animationDuration=".5s"></p-progressSpinner>
`;

export const Custom: Story = () => {
    return {
        template: CustomTemplate,
        props: {
            value: 10,
            showValue: false
        }
    };
};

Custom.parameters = {
    docs: {
        source: {
            code: CustomTemplate
        }
    }
};
