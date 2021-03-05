import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';

export interface Car {
    vin?;
    year?;
    brand?;
    color?;
    price?;
    saleDate?;
}

@Component({
    selector: 'app-p-defer',
    template: `
        <div style="height:1200px">Table is not loaded yet, scroll down to initialize it.</div>

        <p-toast></p-toast>

        <div pDefer (onLoad)="initData()">
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
    cars: [];

    constructor(private messageService: MessageService) {}

    ngOnInit() {
        console.log('object');
    }

    initData() {
        this.messageService.add({
            severity: 'success',
            summary: 'Data Initialized',
            detail: 'Render Completed'
        });
    }
}
