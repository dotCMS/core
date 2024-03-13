import { moduleMetadata } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { TableModule } from 'primeng/table';

import { DotTableComponent } from './dot-table.component';

export default {
    title: 'DotTableComponent',
    component: DotTableComponent,
    decorators: [
        moduleMetadata({
            imports: [TableModule, CommonModule]
        })
    ]
};

export const Default = () => ({
    component: DotTableComponent
});
