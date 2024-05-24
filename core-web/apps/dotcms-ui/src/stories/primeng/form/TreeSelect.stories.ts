import { Meta, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TreeNode } from 'primeng/api';
import { TreeSelectModule, TreeSelect } from 'primeng/treeselect';

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

const files: TreeNode[] = [
    {
        label: 'Documents',
        data: 'Documents Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'Work',
                data: 'Work Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Expenses.doc',
                        icon: 'pi pi-file',
                        data: 'Expenses Document'
                    },
                    { label: 'Resume.doc', icon: 'pi pi-file', data: 'Resume Document' }
                ]
            },
            {
                label: 'Home',
                data: 'Home Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Invoices.txt',
                        icon: 'pi pi-file',
                        data: 'Invoices for this month'
                    }
                ]
            }
        ]
    },
    {
        label: 'Pictures',
        data: 'Pictures Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            { label: 'barcelona.jpg', icon: 'pi pi-image', data: 'Barcelona Photo' },
            { label: 'logo.jpg', icon: 'pi pi-image', data: 'PrimeFaces Logo' },
            { label: 'primeui.png', icon: 'pi pi-image', data: 'PrimeUI Logo' }
        ]
    },
    {
        label: 'Movies',
        data: 'Movies Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'Al Pacino',
                data: 'Pacino Movies',
                children: [
                    { label: 'Scarface', icon: 'pi pi-video', data: 'Scarface Movie' },
                    { label: 'Serpico', icon: 'pi pi-video', data: 'Serpico Movie' }
                ]
            },
            {
                label: 'Robert De Niro',
                data: 'De Niro Movies',
                children: [
                    {
                        label: 'Goodfellas',
                        icon: 'pi pi-video',
                        data: 'Goodfellas Movie'
                    },
                    {
                        label: 'Untouchables',
                        icon: 'pi pi-video',
                        data: 'Untouchables Movie'
                    }
                ]
            }
        ]
    }
];

export const Default: Story = {
    render: () => ({
        moduleMetadata: {
            imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
        },
        props: {
            files,
            selectedNodes: ''
        },
        template: `
        <div class="card flex justify-content-center">
            <p-treeSelect
                [(ngModel)]="selectedNodes"
                class="md:w-20rem w-full" containerStyleClass="w-full"
                [options]="files"
                placeholder="Select Item"
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
        moduleMetadata: {
            imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
        },
        props: {
            files,
            selectedNodes: ''
        },
        template: `
        <div class="card flex justify-content-center">
            <p-treeSelect
                class="md:w-20rem w-full ng-invalid ng-dirty"
                containerStyleClass="w-full"
                [(ngModel)]="selectedNodes"
                [options]="files"
                placeholder="Select Item"
            />
        </div>
        `
    })
};

export const WithLabel: Story = {
    render: () => ({
        moduleMetadata: {
            imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
        },
        props: {
            files,
            selectedNodes: ''
        },
        template: `
        <div class="card flex justify-content-center">
            <span class="md:w-20rem w-full">
                <label for="treeselect">Label</label>
                <p-treeSelect
                    containerStyleClass="w-full"
                    [(ngModel)]="selectedNodes"
                    [options]="files"
                    placeholder="Select Item"
                />
            </span>
        </div>
        `
    })
};

export const WithFloatLabel: Story = {
    render: () => ({
        moduleMetadata: {
            imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
        },
        props: {
            files,
            selectedNodes: '',
            dropdownIcon: 'pi pi-chevron-down'
        },
        template: `
        <div class="card flex justify-content-center">
            <span class="p-float-label md:w-20rem w-full">
                <p-treeSelect
                    containerStyleClass="w-full"
                    [(ngModel)]="selectedNodes"
                    [options]="files"
                    placeholder="Select Item"
                />
                <label for="treeselect">Float Label</label>
            </span>
        </div>
        `
    })
};

export const Disable: Story = {
    render: () => ({
        moduleMetadata: {
            imports: [TreeSelectModule, FormsModule, BrowserAnimationsModule, BrowserModule]
        },
        props: {
            files,
            selectedNodes: ''
        },
        template: `
        <div class="card flex justify-content-center">
            <p-treeSelect
                [disabled]="true"
                [(ngModel)]="selectedNodes"
                class="md:w-20rem w-full" containerStyleClass="w-full"
                [options]="files"
                placeholder="Select Item"
            />
        </div>
        `
    })
};
