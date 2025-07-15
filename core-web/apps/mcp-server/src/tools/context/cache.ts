import { ContentType } from '../../types/contentype';
import { Site } from '../../types/site';
import { WorkflowScheme } from '../../types/workflow';

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let currentSiteCache: Site | null = null;
let workflowSchemesCache: WorkflowScheme[] | null = null;
let cacheTimestamp = 0;

// Cache duration: 30 minutes
const CACHE_DURATION = 30 * 60 * 1000;

/**
 * Cache data structure
 */
export interface CacheData {
    contentTypes: ContentType[];
    site: Site;
    workflowSchemes: WorkflowScheme[];
    timestamp: number;
}

/**
 * Check if cache is valid and return cached data if available
 */
export function getCachedData(): { data: CacheData; age: number } | null {
    const now = Date.now();

    if (
        contentTypeSchemasCache &&
        currentSiteCache &&
        workflowSchemesCache &&
        now - cacheTimestamp < CACHE_DURATION
    ) {
        const cacheAge = Math.round((now - cacheTimestamp) / 1000);

        return {
            data: {
                contentTypes: contentTypeSchemasCache,
                site: currentSiteCache,
                workflowSchemes: workflowSchemesCache,
                timestamp: cacheTimestamp
            },
            age: cacheAge
        };
    }

    return null;
}

/**
 * Store data in cache
 */
export function setCacheData(
    contentTypes: ContentType[],
    site: Site,
    workflowSchemes: WorkflowScheme[]
): void {
    contentTypeSchemasCache = contentTypes;
    currentSiteCache = site;
    workflowSchemesCache = workflowSchemes;
    cacheTimestamp = Date.now();
}

/**
 * Get cache age in seconds since last update
 */
export function getCacheAge(): number {
    return Math.round((Date.now() - cacheTimestamp) / 1000);
}
