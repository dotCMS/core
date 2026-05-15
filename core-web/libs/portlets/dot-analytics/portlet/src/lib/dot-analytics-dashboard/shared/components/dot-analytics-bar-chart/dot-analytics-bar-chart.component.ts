import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

@Component({
    selector: 'dot-analytics-bar-chart',
    imports: [
        CardModule,
        SkeletonModule,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-chart.component.html',
    styleUrl: './dot-analytics-bar-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsBarChartComponent {
    readonly #messageService = inject(DotMessageService);

    readonly $data = input.required<EngagementPlatformMetrics[]>({ alias: 'data' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $maxItems = input<number>(5, { alias: 'maxItems' });

    protected readonly $topItems = computed(() => {
        const data = this.$data();
        const max = this.$maxItems();

        return [...data].sort((a, b) => b.percentage - a.percentage).slice(0, max);
    });

    protected readonly $resolvedCardHeader = computed(() => {
        const title = this.$title();
        if (!title?.trim()) {
            return undefined;
        }

        return this.#messageService.get(title);
    });

    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$topItems().length === 0);
}
