import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/** Platform data item structure */
export interface PlatformDataItem {
    name: string;
    views: number;
    percentage: number;
    time: string;
}

/** Platforms data structure */
export interface PlatformsData {
    device: PlatformDataItem[];
    browser: PlatformDataItem[];
    language: PlatformDataItem[];
}

/**
 * Component to display platform analytics in a tabbed table format.
 * Shows Device, Browser, and Language breakdown with views and time metrics.
 */
@Component({
    selector: 'dot-analytics-platforms-table',
    imports: [
        CommonModule,
        CardModule,
        TabsModule,
        TableModule,
        ProgressBarModule,
        SkeletonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-platforms-table.component.html',
    styleUrl: './dot-analytics-platforms-table.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsPlatformsTableComponent {
    /** Platforms data containing device, browser, and language arrays */
    readonly $platforms = input<PlatformsData | null | undefined>(null, { alias: 'platforms' });

    /** Component status for loading/error states */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    /** Device data from platforms */
    readonly $deviceData = computed(() => this.$platforms()?.device ?? []);

    /** Browser data from platforms */
    readonly $browserData = computed(() => this.$platforms()?.browser ?? []);

    /** Language data from platforms */
    readonly $languageData = computed(() => this.$platforms()?.language ?? []);

    /** Check if component is in loading state */
    readonly $isLoading = computed(() => {
        const status = this.$status();
        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });
}
