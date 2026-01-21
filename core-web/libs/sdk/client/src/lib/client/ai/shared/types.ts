import { DotCMSAISearchResponse, DotCMSBasicContentlet, DotErrorAISearch } from '@dotcms/types';
import { ThenableCallback } from '@dotcms/types/internal';

/**
 * Callback for a fulfilled promise.
 *
 * @template T - The type of the response.
 * @callback OnFullfilled
 * @param {DotCMSAISearchResponse} value - The response value.
 * @returns {DotCMSAISearchResponse | PromiseLike<DotCMSAISearchResponse> | void} The processed response or a promise.
 */
export type OnFullfilled<T extends DotCMSBasicContentlet> = ThenableCallback<
    DotCMSAISearchResponse<T>
>;

/**
 * Callback for a rejected promise.
 *
 * @callback OnRejected
 * @param {DotErrorAISearch} error - The AI search error object.
 * @returns {DotErrorAISearch | PromiseLike<DotErrorAISearch> | void} The processed error or a promise.
 */
export type OnRejected = ThenableCallback<DotErrorAISearch>;
