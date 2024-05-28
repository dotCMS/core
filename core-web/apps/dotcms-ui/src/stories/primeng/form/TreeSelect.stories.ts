import { Meta, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TreeSelectModule, TreeSelect } from 'primeng/treeselect';

import { files } from './../data/Tree.stories';

const meta: Meta<TreeSelect> = {
    title: 'PrimeNG/Form/TreeSelect',
    component: TreeSelect,
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'TreeSelect is a form component to choose from hierarchical data.: https://www.primefaces.org/primeng-v15-lts/treeselect'
            }
        }
    }
};
export default meta;

type Story = StoryObj<TreeSelect>;

const storyConfigBase: Partial<Story['render']> = {
    moduleMetadata: {
        imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
    },
    props: {
        files,
        selectedNodes: ''
    }
};

export const Default: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                [(ngModel)]="selectedNodes"
                class="md:w-20rem w-full" containerStyleClass="w-full"
                [options]="files"
                placeholder="Select Item"
                [showClear]="true"
            >
                <ng-template pTemplate="triggericon">
                    <i class="pi pi-chevron-down"></i>
                </ng-template>
            </p-treeSelect> 
        </div>
        `
    })
};

export const Invalid: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                class="md:w-20rem w-full ng-invalid ng-dirty"
                containerStyleClass="w-full"
                [(ngModel)]="selectedNodes"
                [options]="files"
                placeholder="Select Item"
                [showClear]="true"
            >
                <ng-template pTemplate="triggericon">
                    <i class="pi pi-chevron-down"></i>
                </ng-template>
            </p-treeSelect> 
        </div>
        `
    })
};

export const WithLabel: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `
        <div class="card flex justify-content-center w-25rem h-25rem">
            <span class="md:w-20rem w-full">
                <label for="treeselect">Label</label>
                <p-treeSelect
                    containerStyleClass="w-full"
                    [(ngModel)]="selectedNodes"
                    [options]="files"
                    placeholder="Select Item"
                    [showClear]="true"
                >
                    <ng-template pTemplate="triggericon">
                        <i class="pi pi-chevron-down"></i>
                    </ng-template>
                </p-treeSelect> 
            </span>
        </div>
        `
    })
};

export const WithFloatLabel: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `
        <div class="card flex justify-content-center w-25rem h-25rem">
            <div class="md:w-20rem w-full">
                <span class="p-float-label w-full">
                    <p-treeSelect
                        containerStyleClass="w-full"
                        [(ngModel)]="selectedNodes"
                        [options]="files"
                        placeholder="Select Item"
                        [showClear]="true"
                    >
                        <ng-template pTemplate="triggericon">
                            <i class="pi pi-chevron-down"></i>
                        </ng-template>
                    </p-treeSelect> 
                    <label for="treeselect">Float Label</label>
                </span>
            </div>
        </div>
        `
    })
};

export const Disable: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                [disabled]="true"
                [(ngModel)]="selectedNodes"
                class="md:w-20rem w-full" containerStyleClass="w-full"
                [options]="files"
                placeholder="Select Item"
                [showClear]="true"
            >
                <ng-template pTemplate="triggericon">
                    <i class="pi pi-chevron-down"></i>
                </ng-template>
            </p-treeSelect> 
        </div>
        `
    })
};

export const Multiple: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `  
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                class="w-full md:w-20rem"
                containerStyleClass="w-full"
                [(ngModel)]="selectedNodes"
                [options]="files"
                [metaKeySelection]="false"
                selectionMode="multiple"
                placeholder="Select Item"
                [showClear]="true"
            >
                <ng-template pTemplate="triggericon">
                    <i class="pi pi-chevron-down"></i>
                </ng-template>
            </p-treeSelect> 
        </div>
        `
    })
};

export const Checkbox: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `  
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                class="w-full md:w-20rem"
                containerStyleClass="w-full"
                [(ngModel)]="selectedNodes"
                [options]="files"
                [showClear]="true"
                display="chip"
                [metaKeySelection]="false"
                selectionMode="checkbox"
                placeholder="Select Item"
                [showClear]="true"
            >
                <ng-template pTemplate="triggericon">
                    <i class="pi pi-chevron-down"></i>
                </ng-template>
            </p-treeSelect> 
        </div>
        `
    })
};

export const Filter: Story = {
    render: () => ({
        ...storyConfigBase,
        template: `  
        <div class="card flex justify-content-center w-25rem h-25rem">
            <p-treeSelect
                class="md:w-20rem w-full"
                containerStyleClass="w-full"
                [(ngModel)]="selectedNodes"
                [options]="files"
                placeholder="Select Item"
                [filter]="true"
                [filterInputAutoFocus]="true"
                [showClear]="true"
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
            </p-treeSelect>
        </div>
        `
    })
};
