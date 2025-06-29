import { AgnosticClient } from './client';

import { Site, SiteResponseSchema } from '../types/site';
import { Logger } from '../utils/logger';

export class SiteService extends AgnosticClient {
    private logger: Logger;

    constructor() {
        super();
        this.logger = new Logger('SITE_SERVICE');
    }

    /**
     * Fetches the current site from dotCMS.
     * @returns Promise with the current site information
     */
    async getCurrentSite(): Promise<Site> {
        this.logger.log('Fetching current site from dotCMS');

        const url = `${this.dotcmsUrl}/api/v1/site/currentSite`;
        this.logger.log('Making request to dotCMS server', { url, method: 'GET' });

        try {
            const response = await this.fetch(url, { method: 'GET' });

            this.logger.log('Received response from dotCMS server', {
                status: response.status,
                statusText: response.statusText,
                ok: response.ok
            });

            if (!response.ok) {
                this.logger.error('dotCMS server returned error', {
                    status: response.status,
                    statusText: response.statusText
                });
                throw new Error(
                    `Failed to fetch current site: ${response.status} ${response.statusText}`
                );
            }

            const data = await response.json();
            this.logger.log('Parsed JSON response from dotCMS server', data);

            const parsed = SiteResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.logger.error('Invalid site response format', parsed.error);
                throw new Error(
                    'Invalid site response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.logger.log('Current site fetched successfully', parsed.data.entity);

            return parsed.data.entity;

        } catch (error) {
            this.logger.error('Error fetching current site', error);
            throw new Error('Error fetching current site: ' + error);
        }
    }
}
