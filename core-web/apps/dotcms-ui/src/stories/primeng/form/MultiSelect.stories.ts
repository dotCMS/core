import {
    Meta,
    moduleMetadata,
    argsToTemplate,
    componentWrapperDecorator,
    StoryObj
} from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

const meta: Meta<MultiSelect> = {
    title: 'PrimeNG/Form/MultiSelect',
    component: MultiSelect,
    decorators: [
        moduleMetadata({
            imports: [MultiSelectModule, BrowserAnimationsModule, FormsModule]
        }),
        componentWrapperDecorator(
            (story) =>
                `<div class="card flex justify-content-center w-50rem h-25rem">${story}</div>`
        )
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'MultiSelect is used to multiple values from a list of options: https://primeng.org/multiselect'
            }
        }
    },
    args: {
        options: [
            { name: 'New York', code: 'NY' },
            { name: 'Rome', code: 'RM' },
            { name: 'London', code: 'LDN' },
            { name: 'Istanbul', code: 'IST' },
            { name: 'Paris', code: 'PRS' }
        ],
        placeholder: 'Select a City',
        optionLabel: 'name',
        value: [{ name: 'Paris', code: 'PRS' }]
    },
    render: (args) => ({
        props: {
            ...args
        },
        template: `
        <p-multiSelect
            ${argsToTemplate(args)}
            containerStyleClass="w-full"
            class="w-full md:w-20rem"
        />`
    })
};
export default meta;

type Story = StoryObj<MultiSelect>;

export const Default: Story = {};
