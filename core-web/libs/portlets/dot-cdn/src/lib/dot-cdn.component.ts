import { ChartOptions } from 'chart.js';
import { Observable } from 'rxjs';

import { AsyncPipe, NgStyle } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { TextareaModule } from 'primeng/textarea';

import { take } from 'rxjs/operators';

import { DotIconComponent, DotSpinnerComponent } from '@dotcms/ui';

import { CdnDateFilter, DotCdnFiltersComponent } from './dot-cdn-filters/dot-cdn-filters.component';
import { CdnChartOptions, DotCDNState } from './dot-cdn.models';
import { DotCDNStore } from './dot-cdn.store';

@Component({
    selector: 'dot-cdn',
    templateUrl: './dot-cdn.component.html',
    styleUrls: ['./dot-cdn.component.scss'],
    standalone: true,
    imports: [
        AsyncPipe,
        NgStyle,
        FormsModule,
        ReactiveFormsModule,
        TabsModule,
        ChartModule,
        ButtonModule,
        TextareaModule,
        SkeletonModule,
        DotIconComponent,
        DotSpinnerComponent,
        DotCdnFiltersComponent
    ],
    providers: [DotCDNStore]
})
export class DotCDNComponent {
    private fb = inject(UntypedFormBuilder);
    private dotCdnStore = inject(DotCDNStore);

    purgeZoneForm: UntypedFormGroup = this.fb.group({
        purgeUrlsTextArea: ''
    });
    vm$: Observable<Omit<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vm$;
    vmPurgeLoaders$: Observable<Pick<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vmPurgeLoaders$;
    chartHeight = '25rem';
    options: CdnChartOptions = this.buildChartOptions();

    onFilterChange(filter: CdnDateFilter): void {
        this.dotCdnStore.loadStats(filter);
    }

    purgePullZone(): void {
        this.dotCdnStore.purgeCDNCacheAll();
    }

    purgeUrls(): void {
        const urls: string[] = this.purgeZoneForm.value.purgeUrlsTextArea
            .split('\n')
            .map((url: string) => url.trim())
            .filter((url: string) => url.length > 0);

        if (urls.length === 0) {
            return;
        }

        this.dotCdnStore
            .purgeCDNCache(urls)
            .pipe(take(1))
            .subscribe(() => {
                this.purgeZoneForm.setValue({ purgeUrlsTextArea: '' });
            });
    }

    private buildChartOptions(): CdnChartOptions {
        const bandwidthOptions: ChartOptions = {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.formattedValue}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        maxTicksLimit: 15,
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (value: string | number): string {
                            return Number(value).toLocaleString('en-US');
                        }
                    }
                }
            }
        };

        const requestOptions: ChartOptions = {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${Number(context.raw).toLocaleString('en-US')}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        maxTicksLimit: 15,
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0,
                        callback: function (value: string | number): string {
                            return Number(value).toLocaleString('en-US');
                        }
                    }
                }
            }
        };

        const commonScales = {
            x: { ticks: { maxTicksLimit: 10, maxRotation: 45, minRotation: 0 } },
            y: { beginAtZero: true }
        };

        const cacheHitRateOptions: ChartOptions = {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: (context) =>
                            `${context.dataset.label}: ${Number(context.raw).toFixed(2)}%`
                    }
                }
            },
            scales: {
                ...commonScales,
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        callback: (value: string | number): string => `${value}%`
                    }
                }
            }
        };

        const errorOptions: ChartOptions = {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: (context) =>
                            `${context.dataset.label}: ${Number(context.raw).toLocaleString('en-US')}`
                    }
                }
            },
            scales: {
                ...commonScales,
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0,
                        callback: (value: string | number): string =>
                            Number(value).toLocaleString('en-US')
                    }
                }
            }
        };

        return {
            bandwidthUsedChart: bandwidthOptions,
            requestsServedChart: requestOptions,
            cacheHitRateChart: cacheHitRateOptions,
            errorChart: errorOptions
        };
    }
}
