import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, computed } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';


import { DotUsageService } from '../services/dot-usage.service';

@Component({
    selector: 'lib-dot-usage-shell',
    imports: [
        CommonModule,
        ButtonModule, 
        CardModule,
        MessagesModule,
        SkeletonModule,
        TooltipModule
    ],
    templateUrl: './dot-usage-shell.component.html',
    styleUrl: './dot-usage-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUsageShellComponent implements OnInit {
    private readonly usageService = inject(DotUsageService);

    // Reactive state from service
    readonly summary = this.usageService.summary;
    readonly loading = this.usageService.loading;
    readonly error = this.usageService.error;

    // Computed values for display
    readonly hasData = computed(() => this.summary() !== null);
    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.usageService.getSummary().subscribe({
            next: () => {
                // Data is automatically updated via signals
            },
            error: (error) => {
                console.error('Failed to load usage data:', error);
            }
        });
    }

    onRefresh(): void {
        this.loadData();
    }

    onRetry(): void {
        this.usageService.reset();
        this.loadData();
    }

    formatNumber(num: number): string {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        }
        if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toLocaleString();
    }
}
