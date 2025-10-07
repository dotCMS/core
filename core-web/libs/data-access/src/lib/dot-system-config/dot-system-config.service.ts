import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotSystemConfig, SystemConfigResponse } from '@dotcms/dotcms-models';

/**
 * Service to manage system configuration from DotCMS server.
 *
 * This service provides a modern replacement for the deprecated coreWebService approach,
 * using HttpClient directly to fetch system configuration data. It focuses on the subset
 * of configuration data needed for system-level UI components and global state.
 *
 * @example
 * ```typescript
 * // Inject the service
 * private systemConfigService = inject(DotSystemConfigService);
 *
 * // Get system configuration
 * this.systemConfigService.getSystemConfig().subscribe(config => {
 *   console.log(config.logos, config.colors, config.license);
 * });
 * ```
 */
@Injectable()
export class DotSystemConfigService {
    private readonly http = inject(HttpClient);

    private readonly configUrl = '/api/v1/appconfiguration';

    /**
     * Fetches the system configuration from the server.
     *
     * This method makes a clean HTTP request to get only the essential system configuration
     * data needed for the global store and UI components. It extracts and transforms the
     * relevant data from the full server response.
     *
     * @returns Observable<DotSystemConfig> - Observable containing the system configuration
     *
     * @example
     * ```typescript
     * this.systemConfigService.getSystemConfig().subscribe({
     *   next: (config) => {
     *     console.log('System timezone:', config.systemTimezone);
     *     console.log('Available languages:', config.languages);
     *     console.log('License info:', config.license);
     *   },
     *   error: (error) => {
     *     console.error('Failed to load system config:', error);
     *   }
     * });
     * ```
     */
    getSystemConfig(): Observable<DotSystemConfig> {
        return this.http
            .get<SystemConfigResponse>(this.configUrl)
            .pipe(map((response) => this.extractSystemConfig(response)));
    }

    /**
     * Extracts the relevant system configuration from the server response.
     *
     * This method transforms the full server response into a clean, typed
     * system configuration object containing only the data we need for
     * the global store.
     *
     * @private
     * @param response - The full server response
     * @returns DotSystemConfig - The extracted system configuration
     */
    private extractSystemConfig(response: SystemConfigResponse): DotSystemConfig {
        const { config } = response.entity;

        return {
            logos: config.logos,
            colors: config.colors,
            releaseInfo: config.releaseInfo,
            systemTimezone: config.systemTimezone,
            languages: config.languages,
            license: config.license,
            cluster: config.cluster
        };
    }
}
