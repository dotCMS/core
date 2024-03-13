import { CommonModule, NgSwitch, NgSwitchCase } from '@angular/common';
import { Component } from '@angular/core';

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

@Component({
    selector: 'dot-table',
    standalone: true,
    imports: [TableModule, TagModule, NgSwitch, NgSwitchCase, CommonModule],
    templateUrl: './dot-table.component.html',
    styleUrl: './dot-table.component.scss'
})
export class DotTableComponent {
    products = [
        {
            assignee: 'Floyd Miles',
            name: 'saudi_riyal.flo',
            status: 'Published',
            step: 'Published',
            date: addDays(currentDate, 1).toISOString().split('T')[0]
        },
        {
            assignee: 'Marvin McKinney',
            name: 'collaboration.nsf',
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
            status: 'Published',
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
            status: 'Published',
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
            status: 'Published',
            step: 'Final Review',
            date: addDays(currentDate, 10).toISOString().split('T')[0]
        }
    ];

    status = {
        published: 'p-tag-success',
        archived: 'p-tag-info',
        new: 'p-tag-danger'
    };

    testStatus = 'p-tag-success';
}
