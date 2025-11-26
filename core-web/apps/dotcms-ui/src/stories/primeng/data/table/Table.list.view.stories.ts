import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { CommonModule, NgIf } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

// Get the current date
const currentDate = new Date();

// Function to add a day to a date
const addDays = (date, days) => {
    const result = new Date(date);
    result.setDate(result.getDate() + days);

    return result;
};

interface WorkflowItem {
    assignee: string;
    name: string;
    step: string;
    status: string;
    date: string;
}

const data = [
    {
        assignee: 'Floyd Miles',
        name: 'Chronicles_of_the_Mystical_Empyrean_Expedition_Report_and_Analysis_Document_2024_Final_Version.docx',
        status: 'New',
        step: 'Published',
        date: addDays(currentDate, 1).toISOString().split('T')[0]
    },
    {
        assignee: 'Marvin McKinney',
        name: 'collaboration.nsf',
        default: true,
        status: 'Archived',
        step: 'Draft',
        date: addDays(currentDate, 2).toISOString().split('T')[0]
    },
    {
        assignee: 'Annette Black',
        name: 'virtual_plastic_sleek_cotton_salad.gph',
        status: 'New',
        step: 'Draft',
        date: addDays(currentDate, 3).toISOString().split('T')[0]
    },
    {
        assignee: 'Evelyn Mendoza',
        name: 'product_catalog.xls',
        status: 'New',
        step: 'Final Review',
        date: addDays(currentDate, 4).toISOString().split('T')[0]
    },
    {
        assignee: 'Clarence Soto',
        name: 'inventory_management.docx',
        status: 'Archived',
        step: 'Pending Approval',
        date: addDays(currentDate, 5).toISOString().split('T')[0]
    },
    {
        assignee: 'Kristine Holmes',
        name: 'sales_report.pdf',
        status: 'Archived',
        step: 'Final Review',
        date: addDays(currentDate, 6).toISOString().split('T')[0]
    },
    {
        assignee: 'Travis Harris',
        name: 'customer_feedback.csv',
        status: 'New',
        step: 'Draft',
        date: addDays(currentDate, 7).toISOString().split('T')[0]
    },
    {
        assignee: 'Naomi Allen',
        name: 'marketing_strategy.ppt',
        status: 'New',
        step: 'Pending Approval',
        date: addDays(currentDate, 8).toISOString().split('T')[0]
    },
    {
        assignee: 'Francis George',
        name: 'project_proposal.doc',
        status: 'New',
        step: 'Draft',
        date: addDays(currentDate, 9).toISOString().split('T')[0]
    },
    {
        assignee: 'Beverly Lane',
        name: 'budget_plan.xls',
        status: 'Archived',
        step: 'Final Review',
        date: addDays(currentDate, 10).toISOString().split('T')[0]
    }
] as WorkflowItem[];

const meta: Meta = {
    title: 'PrimeNG/Data/Table/List View',
    decorators: [
        moduleMetadata({
            imports: [TableModule, CommonModule, TagModule, ButtonModule, NgIf]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Table displays data in tabular format. List View Version: https://www.primefaces.org/primeng-v15-lts/table'
            }
        }
    }
};
export default meta;

type Story = StoryObj;

export const Default: Story = {
    render: () => {
        return {
            props: {
                data
            },
            template: `
        <p-table [value]="data" styleClass="dotTable">
            <ng-template pTemplate="caption">
                <span>List of Documents</span>
            </ng-template>
            <ng-template pTemplate="header">
                <tr>
                    <th>
                        <p-tableHeaderCheckbox></p-tableHeaderCheckbox>
                    </th>
                    <th pSortableColumn="name">Title <p-sortIcon field="name"></p-sortIcon></th>
                    <th pSortableColumn="status">Status <p-sortIcon field="status"></p-sortIcon></th>
                    <th pSortableColumn="assignee">Assignee <p-sortIcon field="assignee"></p-sortIcon></th>
                    <th pSortableColumn="step">Step <p-sortIcon field="step"></p-sortIcon></th>
                    <th pSortableColumn="date">Last Updated <p-sortIcon field="date"></p-sortIcon></th>
                    <th style="width: 50px">Menu</th>
                </tr>
            </ng-template>
            <ng-template pTemplate="body" let-workflowItem>
                <tr [pSelectableRow]="workflowItem" [pSelectableRowDisabled]="true">
                    <td>
                        <p-tableCheckbox [value]="workflowItem"></p-tableCheckbox>
                    </td>
                    <td [ngClass]="{'tag-padding': workflowItem.default }" ><span [attr.data-wont-fit]="workflowItem.name">{{ workflowItem.name }}</span> <p-tag *ngIf="workflowItem.default"  class="sm p-tag-success" value="Default"/></td>
                    <td><p-tag  class="sm p-tag-success" [value]="workflowItem.status"/></td>
                    <td><span [attr.data-wont-fit]="workflowItem.assignee" >{{ workflowItem.assignee }}</span></td>
                    <td>{{ workflowItem.step }}</td>
                    <td>{{ workflowItem.date | date }}</td>
                    <td>
                        <i class="pi pi-ellipsis-v"></i>
                    </td>
                </tr>
            </ng-template>
             <ng-template pTemplate="summary">
                    <span>In total there are ## documents.</span>
            </ng-template>
        </p-table>
  `
        };
    }
};
