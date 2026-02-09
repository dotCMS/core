import { Component, Input, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { DeferModule } from 'primeng/defer';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

export interface Car {
    vin: string;
    year: number;
    brand: string;
    color: string;
}

@Component({
    selector: 'dot-p-defer',
    imports: [ToastModule, TableModule, DeferModule],
    template: `
        <div style="height:1200px">Table is not loaded yet, scroll down to initialize it.</div>

        <p-toast></p-toast>

        <div (onLoad)="initData()" pDefer>
            <ng-template>
                <p-table [value]="cars">
                    <ng-template pTemplate="header">
                        <tr>
                            <th>Vin</th>
                            <th>Year</th>
                            <th>Brand</th>
                            <th>Color</th>
                        </tr>
                    </ng-template>
                    <ng-template pTemplate="body" let-car>
                        <tr>
                            <td>{{ car.vin }}</td>
                            <td>{{ car.year }}</td>
                            <td>{{ car.brand }}</td>
                            <td>{{ car.color }}</td>
                        </tr>
                    </ng-template>
                </p-table>
            </ng-template>
        </div>
    `
})
export class DeferComponent {
    private messageService = inject(MessageService);

    @Input() cars: Car[] = [];

    initData() {
        this.messageService.add({
            severity: 'success',
            summary: 'Data Initialized',
            detail: 'Render Completed'
        });
    }
}
