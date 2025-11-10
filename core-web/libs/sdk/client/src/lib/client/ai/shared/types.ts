import { DotCMSBasicContentlet, DotErrorAISearch } from '@dotcms/types';

/**
 * Callback for a fulfilled promise.
 *
 * @template T - The type of the response.
 * @callback OnFullfilled
 * @param {DotCMSAISearchResponse} value - The response value.
 * @returns {DotCMSAISearchResponse | PromiseLike<DotCMSAISearchResponse>} The processed response or a promise.
 */
export type OnFullfilled<T extends DotCMSBasicContentlet> =
    | ((
          value: DotCMSAISearchResponse<T>
      ) => DotCMSAISearchResponse<T> | PromiseLike<DotCMSAISearchResponse<T>>)
    | undefined
    | null;

/**
 * Callback for a rejected promise.
 *
 * @callback OnRejected
 * @param {DotErrorAISearch} error - The AI search error object.
 * @returns {DotErrorAISearch | PromiseLike<DotErrorAISearch>} The processed error or a promise.
 */
export type OnRejected =
    | ((error: DotErrorAISearch) => DotErrorAISearch | PromiseLike<DotErrorAISearch>)
    | undefined
    | null;

export interface DotCMSAISearchResponse<T extends DotCMSBasicContentlet> {
    dotCMSResults: DotCMSAISearchContentletData<T>[];
}

export interface DotCMSAISearchMatch {
    distance: number;
    extractedText: string;
}

export type DotCMSAISearchContentletData<T extends DotCMSBasicContentlet> = T & {
    matches?: DotCMSAISearchMatch[];
};
