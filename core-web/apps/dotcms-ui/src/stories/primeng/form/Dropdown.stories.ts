import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { NgStyle } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Dropdown, DropdownModule } from 'primeng/dropdown';

type Args = Dropdown & { width: string };

const DropdownTemplate = `
    <p><p-dropdown [options]="options" showClear="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-dropdown></p>
    <p><p-dropdown [options]="options" showClear="true" [editable]="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-dropdown></p>
    <p><p-dropdown [options]="options" showClear="true" [filter]="true" filterBy="label" [editable]="true" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-dropdown></p>
    <p><p-dropdown [options]="options" [disabled]="true" [style]="{'width': width + 'px'}"></p-dropdown></p>
    <hr />
    <p><p-dropdown class="p-dropdown-sm" [options]="options" [style]="{'width': width + 'px'}" optionDisabled="inactive"></p-dropdown></p>
`;

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/Dropdown',
    component: Dropdown,
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
            imports: [DropdownModule, BrowserAnimationsModule, NgStyle]
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
        template: DropdownTemplate
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {
    parameters: {
        docs: {
            source: {
                code: DropdownTemplate
            },
            iframeHeight: 300
        }
    }
};

export const CustomTemplate: Story = {
    render: (args) => ({
        props: args,
        template: `
        <p-dropdown [options]="options" [style]="{'width': width + 'px'}">
            <ng-template let-selected pTemplate="selectedItem">
                --{{ selected.label }}--
            </ng-template>
            <ng-template let-item pTemplate="item">
                **{{ item.label }}**
            </ng-template>
        </p-dropdown>`
    })
};
