import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate
} from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TreeSelectModule, TreeSelect } from 'primeng/treeselect';

import { generateFakeTree } from '../../utils/tree-node-files';

type ExtraArgs = {
    invalid: boolean;
};

type Args = TreeSelect & ExtraArgs;

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/TreeSelect',
    decorators: [
        moduleMetadata({
            imports: [TreeSelectModule, FormsModule, BrowserModule, BrowserAnimationsModule]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-center w-[25rem] h-25rem">${story}</div>`
        )
    ],
    component: TreeSelect,
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'TreeSelect is a form component to choose from hierarchical data.: https://primeng.org/treeselect'
            }
        }
    },
    args: {
        placeholder: 'Select Item',
        options: [...generateFakeTree()],
        showClear: true,
        invalid: false,
        selectionMode: 'single'
    },
    render: (args) => ({
        props: args,
        template: `
        <p-treeSelect
            ${argsToTemplate(args)}
            containerStyleClass="w-full"
            class="w-full md:w-80"
            [class.ng-invalid]="invalid"
            [class.ng-dirty]="invalid"
        >
            <ng-template pTemplate="triggericon">
                <i class="pi pi-chevron-down"></i>
            </ng-template>
            <ng-template pTemplate="filtericon">
                <i class="pi pi-search"></i>
            </ng-template>
            <ng-template pTemplate="closeicon">
                <i class="pi pi-times"></i>
            </ng-template>
        </p-treeSelect> `
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {};

export const Invalid: Story = {
    args: {
        invalid: true
    }
};

export const Disable: Story = {
    args: {
        disabled: true
    }
};

export const Multiple: Story = {
    args: {
        selectionMode: 'multiple'
    }
};

export const Checkbox: Story = {
    args: {
        selectionMode: 'checkbox'
    }
};

export const Filter: Story = {
    args: {
        filter: true,
        filterInputAutoFocus: true
    }
};

export const WithLabel: Story = {
    decorators: [
        componentWrapperDecorator(
            (story) =>
                `<div class="card flex justify-center w-[25rem] h-[25rem]">
                    <span class="md:w-80 w-full">
                        <label for="treeselect">Label</label>
                        ${story}
                    </span>
                </div>`
        )
    ]
};

export const WithFloatLabel: Story = {
    decorators: [
        componentWrapperDecorator(
            (story) =>
                `<div class="md:w-80 w-full">
                    <span class="p-float-label w-full">
                        ${story}
                        <label for="treeselect">Label</label>
                    </span>
                </div>`
        )
    ]
};
