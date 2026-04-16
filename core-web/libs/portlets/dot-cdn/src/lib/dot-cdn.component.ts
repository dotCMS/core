import { ChartOptions } from 'chart.js';
import { Observable } from 'rxjs';

import { AsyncPipe, NgStyle } from '@angular/common';
import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { UIChart, ChartModule } from 'primeng/chart';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { TextareaModule } from 'primeng/textarea';

import { take } from 'rxjs/operators';

import { DotIconComponent, DotSpinnerComponent } from '@dotcms/ui';

import { CdnChartOptions, ChartPeriod, DotCDNState } from './dot-cdn.models';
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
        SelectModule,
        ButtonModule,
        TextareaModule,
        SkeletonModule,
        DotIconComponent,
        DotSpinnerComponent
    ],
    providers: [DotCDNStore]
})
export class DotCDNComponent implements OnInit {
    private fb = inject(UntypedFormBuilder);
    private dotCdnStore = inject(DotCDNStore);

    @ViewChild('chart', { static: true }) chart: UIChart;
    purgeZoneForm: UntypedFormGroup;
    periodValues: SelectItem[] = [
        { label: 'Last 15 days', value: ChartPeriod.Last15Days },
        { label: 'Last 30 days', value: ChartPeriod.Last30Days },
        { label: 'Last 60 days', value: ChartPeriod.Last60Days },
        // TODO: Remove mock options before merging
        { label: '[MOCK] High traffic (GB)', value: 'mock-high' },
        { label: '[MOCK] Medium traffic (MB)', value: 'mock-medium' },
        { label: '[MOCK] Low traffic (KB)', value: 'mock-low' }
    ];
    selectedPeriod: SelectItem<string> = { value: ChartPeriod.Last15Days };
    vm$: Observable<Omit<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vm$;
    vmPurgeLoaders$: Observable<Pick<DotCDNState, 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'>> =
        this.dotCdnStore.vmPurgeLoaders$;
    chartHeight = '25rem';
    options: CdnChartOptions;

    ngOnInit(): void {
        this.setChartOptions();

        this.purgeZoneForm = this.fb.group({
            purgeUrlsTextArea: ''
        });
    }

    changePeriod(element: HTMLTextAreaElement): void {
        this.dotCdnStore.getChartStats(element.value);
    }

    purgePullZone(): void {
        this.dotCdnStore.purgeCDNCacheAll();
    }

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
                        maxTicksLimit: 15
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (value: number): string {
                            return value.toLocaleString('en-US');
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
                        maxTicksLimit: 15
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (value: number): string {
                            return value.toLocaleString('en-US');
                        }
                    }
                }
            }
        };

        this.options = { bandwidthUsedChart: bandwidthOptions, requestsServedChart: requestOptions };
    }
}
