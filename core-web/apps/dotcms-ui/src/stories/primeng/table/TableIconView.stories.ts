import { Meta, StoryFn, moduleMetadata } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { data } from './Table.stories.mock';

export default {
    title: 'PrimeNG/Tables/IconView Table',
    decorators: [
        moduleMetadata({
            imports: [TableModule, CommonModule, TagModule, ButtonModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Table is a container component to display data in tabular format.: https://primeng.org/table'
            }
        }
    }
} as Meta;

const rows = 8;
export const Default: StoryFn = () => ({
    props: {
        data,
        first: 0,
        rows,
        total: Math.ceil(data.length / rows),
        currentPage: 1,
        prev(): void {
            this.first = this.first - this.rows;
            this.currentPage = this.currentPage - 1;
        },
        next(): void {
            this.first = this.first + this.rows;
            this.currentPage = this.currentPage + 1;
        },
        isFirstPage(): boolean {
            return this.data ? this.first === 0 : true;
        },
        isLastPage(): boolean {
            return this.data ? this.first >= this.data.length - this.rows : true;
        },
        onPageChange(event) {
            this.first = event.first;
            this.rows = event.rows;
        }
    },
    template: `
        <div class="mb-3 flex justify-content-end">
            <div class="dot-table-current-page">{{currentPage}} of {{total}}</div>
            <p-button type="button" icon="pi pi-chevron-left" (click)="prev()" [disabled]="isFirstPage()" styleClass="p-button-text"></p-button>
            <p-button type="button" icon="pi pi-chevron-right" (click)="next()" [disabled]="isLastPage()" styleClass="p-button-text"></p-button>
        </div> 
        <p-table   
            [value]="data"
            [paginator]="true"
            [showCurrentPageReport]="true"
            [rows]="rows"
            [first]="first"
            (onPage)="onPageChange($event)"
            [paginatorPosition]="null"
            [styleClass]="'p-datatable-sm'"
             styleClass="p-datatable-striped">
        <ng-template pTemplate="header">
            <tr>
                <th>
                    <p-tableHeaderCheckbox></p-tableHeaderCheckbox>
                </th>
                <th pSortableColumn="name">Icon <p-sortIcon field="name"></p-sortIcon></th>
                <th pSortableColumn="status">Status <p-sortIcon field="status"></p-sortIcon></th>
                <th pSortableColumn="assignee">Assignee <p-sortIcon field="assignee"></p-sortIcon></th>
                <th pSortableColumn="step">Step <p-sortIcon field="step"></p-sortIcon></th>
                <th pSortableColumn="date">Last Updated <p-sortIcon field="date"></p-sortIcon></th>
                <th>Menu</th>
            </tr>
        </ng-template>
        <ng-template pTemplate="body" let-workflowItem>
            <tr>
                <td>
                    <p-tableCheckbox [value]="workflowItem"></p-tableCheckbox>
                </td>
                <td>
                    <img class="dot-table-image-view" src="http://localhost:8080/dA/c56e5030-fc88-480c-9b2e-4582fd762437/image/300h/50q/gallery-cobbles-9-768x542.jpg"  />
                </td>
                <td><p-tag  class="sm p-tag-success">{{ workflowItem.status }}</p-tag></td>
                <td>{{ workflowItem.assignee }}</td>
                <td>{{ workflowItem.step }}</td>
                <td>{{ workflowItem.date | date }}</td>
                <td>
                    <i class="pi pi-ellipsis-v"></i>
                </td>
            </tr>
        </ng-template>
        </p-table>
  `
});
