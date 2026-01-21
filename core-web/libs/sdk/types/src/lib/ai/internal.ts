import { DISTANCE_FUNCTIONS, DotCMSAISearchContentletData } from './public';

import { DotCMSBasicContentlet } from '../page/public';

/**
 * The raw response from the AI search.
 *
 * @export
 * @interface DotCMSAISearchRawResponse
 */
export interface DotCMSAISearchRawResponse<T extends DotCMSBasicContentlet> {
    /**
     * The time to embeddings.
     * @property {number} timeToEmbeddings - The time to embeddings.
     */
    timeToEmbeddings: number;
    /**
     * The total number of results.
     * @property {number} total - The total number of results.
     */
    total: number;
    /**
     * The query that was used to search.
     * @property {string} query - The query.
     */
    query: string;
    /**
     * The threshold that was used to calculate the distance.
     * @property {number} threshold - The threshold.
     */
    threshold: number;
    /**
     * The operator that was used to calculate the distance.
     * @property {(typeof DISTANCE_FUNCTIONS)[keyof typeof DISTANCE_FUNCTIONS]} operator - The operator.
     */
    operator: (typeof DISTANCE_FUNCTIONS)[keyof typeof DISTANCE_FUNCTIONS];
    /**
     * The offset of the results.
     * @property {number} offset - The offset.
     */
    offset: number;
    /**
     * The limit of the results.
     * @property {number} limit - The limit.
     */
    limit: number;
    /**
     * The count of the results.
     * @property {number} count - The count.
     */
    count: number;

    /**
     * The dotCMS results.
     * @property {DotCMSAISearchContentletData<T>[]} dotCMSResults - The dotCMS results.
     */
    dotCMSResults: DotCMSAISearchContentletData<T>[];
}
