import { Meta, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Chips, ChipsModule } from 'primeng/chips';

const meta: Meta = {
    title: 'PrimeNG/Form/Chips',
    component: Chips,
    parameters: {
        docs: {
            description: {
                component:
                    'Chips is used to enter multiple values on an input field: https://primeng.org/chips'
            }
        }
    },
    argTypes: {
        values: {
            name: 'values',
            description: 'Array of strings, each representing a chip.'
        }
    },
    args: {
        values: ['one', 'two']
    }
};
export default meta;

type Story = StoryObj<Chips>;

export const Default: Story = {
    render: (args) => ({
        props: args,
        moduleMetadata: {
            imports: [ChipsModule, BrowserAnimationsModule, FormsModule]
        },
        template: `<p-chips [(ngModel)]="values" />`
    })
};
