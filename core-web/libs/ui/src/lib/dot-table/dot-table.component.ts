import { Component } from '@angular/core';

import { TableModule } from 'primeng/table';

@Component({
    selector: 'dot-table',
    standalone: true,
    imports: [TableModule],
    templateUrl: './dot-table.component.html',
    styleUrl: './dot-table.component.scss'
})
export class DotTableComponent {
    products = [
        {
            code: 1,
            name: 'Computer',
            price: 1200
        },
        {
            code: 2,
            name: 'Printer',
            price: 200
        }
    ];
}
