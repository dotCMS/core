import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { NgStyle } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Select, SelectModule } from 'primeng/select';

type Args = Select & { width: string };

const SelectTemplate = `
    <p><p-select [options]="options" showClear="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-select></p>
    <p><p-select [options]="options" showClear="true" [editable]="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-select></p>
    <p><p-select [options]="options" showClear="true" [filter]="true" filterBy="label" [editable]="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-select></p>
    <p><p-select [options]="options" [disabled]="true" [style]="{'width': width + 'px'}"></p-select></p>
    <hr />
    <p><p-select class="p-select-sm" [options]="options" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-select></p>
`;

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/Dropdown',
    component: Select,
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Dropdown is used to select an item from a list of options: https://primeng.org/dropdown'
            }
        }
    },

    decorators: [
        moduleMetadata({
            imports: [SelectModule, BrowserAnimationsModule, NgStyle]
        })
    ],
    argTypes: {
        width: {
            name: 'width',
            type: { name: 'string', required: true },
            defaultValue: '300',
            description:
                "Setting a width prevents the dropdown from jumping when an option is larger than the dropdown's width",
            control: {
                type: 'text'
            }
        }
    },
    args: {
        options: [
            { label: 'Select City', value: null, inactive: true },
            { label: 'New York', value: { id: 1, name: 'New York', code: 'NY' } },
            { label: 'Rome', value: { id: 2, name: 'Rome', code: 'RM' } },
            { label: 'London', value: { id: 3, name: 'London', code: 'LDN' } },
            { label: 'Istanbul', value: { id: 4, name: 'Istanbul', code: 'IST' } },
            { label: 'Paris', value: { id: 5, name: 'Paris', code: 'PRS' } }
        ],
        width: '300'
    },
    render: (args) => ({
        props: args,
        template: SelectTemplate
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {
    parameters: {
        docs: {
            source: {
                code: SelectTemplate
            },
            iframeHeight: 300
        }
    }
};

export const CustomTemplate: Story = {
    render: (args) => ({
        props: args,
        template: `
        <p-select [options]="options" [style]="{'width': width + 'px'}">
            <ng-template let-selected pTemplate="selectedItem">
                --{{ selected.label }}--
            </ng-template>
            <ng-template let-item pTemplate="item">
                **{{ item.label }}**
            </ng-template>
        </p-select>`
    })
};
