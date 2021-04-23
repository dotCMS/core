import { Component, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { ChartData, ChartOptions, SelectValues } from './app.interface';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
    @ViewChild('chart', { static: true }) chart: any;
    content$: Observable<{ label: string; value: string }[]>;

    values: SelectValues[] = [
        { name: 'Last 30 days', value: 'last_30' },
        { name: 'Last 60 days', value: 'last_60' }
    ];

    data: ChartData | Record<string, unknown> = {};
    options: ChartOptions | Record<string, unknown> = {};

    ngOnInit(): void {
        this.setData();
        this.setOptions();
    }

    private setOptions(): void {
        this.options = {
            title: {
                display: true,
                text: 'My Title',
                fontSize: 16
            },
            legend: {
                position: 'bottom'
            }
        };
    }

    private setData(): void {
        this.data = {
            labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
            datasets: [
                {
                    label: 'First Dataset',
                    data: [65, 59, 80, 81, 56, 55, 40],
                    borderColor: '#42A5F5',
                    fill: false
                },
                {
                    label: 'Second Dataset',
                    data: [28, 48, 40, 19, 86, 27, 90],
                    borderColor: '#FFA726',
                    fill: false
                }
            ]
        };
    }
}
