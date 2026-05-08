import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotAnalyticsService } from '@dotcms/portlets/dot-analytics/data-access';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';
/**
 * Component that displays error states for analytics when the service is not properly configured.
 * Shows appropriate messages based on enterprise license status and health check results.
 */
@Component({
    selector: 'dot-analytics-error',
    imports: [DotEmptyContainerComponent, DotMessagePipe],
    templateUrl: './dot-analytics-error.component.html'
})
export default class DotAnalyticsErrorComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly analyticsService = inject(DotAnalyticsService);

    /**
     * Computed configuration for the empty state component based on route parameters
     */
    protected readonly $errorConfig = computed((): PrincipalConfiguration => {
        const queryParams = this.route.snapshot.queryParams;
        const status = queryParams['status'] as HealthStatusTypes;
        const isEnterprise = queryParams['isEnterprise'] === 'true';

        return this.getErrorConfig(status, isEnterprise);
    });

    /**
     * Clears the health check cache and navigates back to analytics dashboard.
     */
    onRetry(): void {
        this.analyticsService.clearHealthCache();
        this.router.navigate(['/analytics']);
    }

    /**
     * Gets the appropriate error configuration based on health status and enterprise license
     */
    private getErrorConfig(
        status: HealthStatusTypes,
        isEnterprise: boolean
    ): PrincipalConfiguration {
        if (!isEnterprise) {
            return {
                title: this.dotMessageService.get('analytics.search.no.license'),
                subtitle: this.dotMessageService.get('analytics.search.no.license.subtitle'),
                icon: 'pi-lock'
            };
        }

        const defaultConfig: PrincipalConfiguration = {
            title: this.dotMessageService.get('analytics.error.not.available'),
            subtitle: this.dotMessageService.get('analytics.error.not.available.subtitle'),
            icon: 'pi-exclamation-triangle'
        };

        const enterpriseConfigs: Partial<Record<HealthStatusTypes, PrincipalConfiguration>> = {
            [HealthStatusTypes.NOT_AVAILABLE]: defaultConfig,
            [HealthStatusTypes.NOT_CONFIGURED]: {
                title: this.dotMessageService.get('analytics.search.no.configured'),
                subtitle: this.dotMessageService.get('analytics.search.no.configured.subtitle'),
                icon: 'pi-cog'
            },
            [HealthStatusTypes.CONFIGURATION_ERROR]: {
                title: this.dotMessageService.get('analytics.search.config.error'),
                subtitle: this.dotMessageService.get('analytics.search.config.error.subtitle'),
                icon: 'pi-exclamation-triangle'
            },
            [HealthStatusTypes.OK]: {
                title: this.dotMessageService.get('analytics.search.unexpected.error'),
                subtitle: this.dotMessageService.get('analytics.search.unexpected.error.subtitle'),
                icon: 'pi-times-circle'
            }
        };

        return enterpriseConfigs[status] ?? defaultConfig;
    }
}
