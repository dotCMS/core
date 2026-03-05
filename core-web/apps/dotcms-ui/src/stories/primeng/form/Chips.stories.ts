import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Chips, ChipModule } from 'primeng/chip';

const meta: Meta = {
    title: 'PrimeNG/Form/Chips',
    component: Chips,
    decorators: [
        moduleMetadata({
            imports: [ChipModule, BrowserAnimationsModule, FormsModule]
        })
    ],
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
    },
    render: (args) => ({
        props: args,
        template: `<p-chips [(ngModel)]="values" />`
    })
};
export default meta;

type Story = StoryObj<Chips>;

export const Default: Story = {};
