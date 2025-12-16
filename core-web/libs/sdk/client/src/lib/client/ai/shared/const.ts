import { DISTANCE_FUNCTIONS } from '@dotcms/types';

/**
 * Default values for AI configuration
 */
export const DEFAULT_AI_CONFIG = {
    threshold: 0.5,
    distanceFunction: DISTANCE_FUNCTIONS.cosine,
    responseLength: 1024
} as const;

/**
 * Default values for search query
 */
export const DEFAULT_QUERY = {
    limit: 1000,
    offset: 0,
    indexName: 'default'
} as const;
