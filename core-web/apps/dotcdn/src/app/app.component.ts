import { ChartOptions } from 'chart.js';
import { Observable } from 'rxjs';

import { Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { UIChart } from 'primeng/chart';

import { take } from 'rxjs/operators';

import { CdnChartOptions, ChartPeriod, DotCDNState } from './app.models';
import { DotCDNStore } from './dotcdn.component.store';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
    @ViewChild('chart', { static: true }) chart: UIChart;
    purgeZoneForm: UntypedFormGroup;
    periodValues: SelectItem[] = [
        { label: 'Last 15 days', value: ChartPeriod.Last15Days },
        { label: 'Last 30 days', value: ChartPeriod.Last30Days },
        { label: 'Last 60 days', value: ChartPeriod.Last60Days }
    ];
    selectedPeriod: SelectItem<string> = { value: ChartPeriod.Last15Days };
    vm$: Observable<Omit<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vm$;
    vmPurgeLoaders$: Observable<Pick<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vmPurgeLoaders$;
    chartHeight = '25rem';
    options: CdnChartOptions;

    constructor(
        private fb: UntypedFormBuilder,
        private dotCdnStore: DotCDNStore
    ) {}

    ngOnInit(): void {
        this.setChartOptions();

        this.purgeZoneForm = this.fb.group({
            purgeUrlsTextArea: ''
        });
    }

    /**
     *  Handles the period change
     *
     * @param {*} event
     * @memberof AppComponent
     */
    changePeriod(element: HTMLTextAreaElement): void {
        this.dotCdnStore.getChartStats(element.value);
    }

    /**
     * Purges the entire cache
     *
     * @memberof AppComponent
     */
    purgePullZone(): void {
        this.dotCdnStore.purgeCDNCacheAll();
    }

    /**
     * Purges all the URLs in the array
     *
     * @memberof AppComponent
     */
    purgeUrls(): void {
        const urls: string[] = this.purgeZoneForm
            .get('purgeUrlsTextArea')
            .value.split('\n')
            .map((url) => url.trim());

        this.dotCdnStore
            .purgeCDNCache(urls)
            .pipe(take(1))
            .subscribe(() => {
                this.purgeZoneForm.setValue({ purgeUrlsTextArea: '' });
            });
    }

    private setChartOptions(): void {
        const defaultOptions: ChartOptions = {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.formattedValue}MB`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        maxTicksLimit: 15
                    }
                },
                'y-axis-1': {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    ticks: {
                        callback: function (value: number): string {
                            return value.toString() + 'MB';
                        }
                    }
                }
            }
        };

        const requestOptions: ChartOptions = {
            ...defaultOptions,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.formattedValue}MB`;
                        }
                    }
                }
            },
            scales: {
                ...defaultOptions.scales,
                x: {
                    ...defaultOptions.scales.x,
                    ticks: {
                        callback: (value: number): string => {
                            return Math.round(value).toString();
                        }
                    }
                }
            }
        };

        this.options = { bandwidthUsedChart: defaultOptions, requestsServedChart: requestOptions };
    }
}
