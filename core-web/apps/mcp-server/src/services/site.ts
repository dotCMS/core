import { AgnosticClient } from './client';

import { Site, SiteResponseSchema } from '../types/site';

export class SiteService extends AgnosticClient {
    constructor() {
        super();
    }

    /**
     * Fetches the current site from dotCMS.
     * @returns Promise with the current site information
     */
    async getCurrentSite(): Promise<Site> {
        const url = `${this.dotcmsUrl}/api/v1/site/currentSite`;
        const response = await this.fetch(url, { method: 'GET' });

        if (!response.ok) {
            throw new Error(
                `Failed to fetch current site: ${response.status} ${response.statusText}`
            );
        }

        const data = await response.json();
        const parsed = SiteResponseSchema.safeParse(data);

        if (!parsed.success) {
            throw new Error(
                'Invalid site response: ' + JSON.stringify(parsed.error.format())
            );
        }

        return parsed.data.entity;
    }
}
