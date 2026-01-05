import { z } from 'zod';

import { AgnosticClient } from './client';
import { Logger } from '../utils/logger';

/**
 * Parameters for rendering a page as HTML
 */
export const PageRenderParamsSchema = z.object({
    uri: z.string().min(1, 'uri is required').describe('Page URI, e.g. / or /about-us')
});

export type PageRenderParams = z.infer<typeof PageRenderParamsSchema>;

/**
 * Result of rendering a page as HTML
 */
export type PageRenderResponse = {
    html: string;
    contentType: string;
};

/**
 * Service for rendering pages via dotCMS
 * Hits /api/v1/page/renderHTML/{uri} and returns the HTML as text
 */
export class PageService extends AgnosticClient {
    private serviceLogger: Logger;
    private readonly API_BASE_URL = '/api/v1/page';
    private readonly RENDER_HTML_SEGMENT = 'renderHTML';

    constructor() {
        super();
        this.serviceLogger = new Logger('PAGE_SERVICE');
    }

    /**
     * Build a page API URL by joining path segments and encoding safely.
     * Preserves slashes within a segment and encodes other characters.
     */
    private buildEndpointUrl(...segments: string[]): string {
        const encodedSegments = segments
            .filter((seg) => typeof seg === 'string' && seg.length > 0)
            .map((seg) => encodeURI(seg));
        return [this.API_BASE_URL, ...encodedSegments].join('/');
    }

    /**
     * Normalize incoming URI to a path segment for endpoints that expect `{uri}` after the action.
     * - trims whitespace
     * - strips leading slash (server route already includes a slash before segment)
     */
    private normalizeUriPath(uri: string): string {
        const trimmed = uri.trim();
        return trimmed.startsWith('/') ? trimmed.slice(1) : trimmed;
    }

    /**
     * Perform a GET expecting text response, returning text and content-type.
     * Accept header can be overridden per endpoint if needed.
     */
    private async getText(
        url: string,
        accept = 'text/html, text/plain, */*;q=0.8'
    ): Promise<{ text: string; contentType: string }> {
        const response = await this.fetch(url, {
            method: 'GET',
            headers: {
                Accept: accept
            }
        });

        const contentType = response.headers.get('content-type') || 'text/html';
        const text = await response.text();
        return { text, contentType };
    }

    /**
     * Renders the given URI as HTML using dotCMS page rendering endpoint.
     *
     * Note: The endpoint returns HTML (text/html) rather than JSON. We capture the
     * raw HTML string and the response Content-Type header for callers to use.
     */
    async renderHtml(params: PageRenderParams): Promise<PageRenderResponse> {
        this.serviceLogger.log('Starting page render operation', params);

        const validated = PageRenderParamsSchema.safeParse(params);
        if (!validated.success) {
            this.serviceLogger.error('Invalid page render parameters', validated.error);
            throw new Error(
                'Invalid page render parameters: ' + JSON.stringify(validated.error.format())
            );
        }

        const pathSegment = this.normalizeUriPath(validated.data.uri);
        const url = this.buildEndpointUrl(this.RENDER_HTML_SEGMENT, pathSegment);

        try {
            this.serviceLogger.log('Sending page render request to dotCMS', { url });
            const { text, contentType } = await this.getText(url);

            this.serviceLogger.log('Page HTML fetched successfully', {
                contentType,
                length: text.length
            });

            return { html: text, contentType };
        } catch (error) {
            this.serviceLogger.error('Error during page render operation', error);
            throw error;
        }
    }
}


