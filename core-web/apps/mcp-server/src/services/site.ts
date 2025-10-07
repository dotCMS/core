import { AgnosticClient } from './client';

import { Site, SiteResponseSchema } from '../types/site';
import { Logger } from '../utils/logger';

export class SiteService extends AgnosticClient {
    private serviceLogger: Logger;

    constructor() {
        super();
        this.serviceLogger = new Logger('SITE_SERVICE');
    }

    /**
     * Fetches the current site from dotCMS.
     * @returns Promise with the current site information
     */
    async getCurrentSite(): Promise<Site> {
        this.serviceLogger.log('Fetching current site from dotCMS');

        const url = '/api/v1/site/currentSite';

        try {
            const response = await this.fetch(url, { method: 'GET' });
            const data = await response.json();

            const parsed = SiteResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid site response format', parsed.error);
                throw new Error('Invalid site response: ' + JSON.stringify(parsed.error.format()));
            }

            this.serviceLogger.log('Current site fetched successfully', parsed.data.entity);

            return parsed.data.entity;
        } catch (error) {
            this.serviceLogger.error('Error fetching current site', error);
            throw error;
        }
    }
}
