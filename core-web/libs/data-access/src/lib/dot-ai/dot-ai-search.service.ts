import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotAiCompletionsForm,
    DotAiSearchResponse,
    DotAiSearchResult
} from '@dotcms/dotcms-models';

import { AI_API_ENDPOINT } from './dot-ai.constants';

/** A missing index is a normal, actionable outcome — not a generic failure. */
export interface DotAiIndexNotFoundError {
    indexNotFound: true;
    indexName: string;
    original: HttpErrorResponse;
}

const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

const INDEX_NOT_FOUND = /Index '(.+?)' not found/;

/**
 * Field readers that check the runtime type instead of asserting it.
 *
 * `as string` / `as number` on a wire value is a promise the compiler cannot keep: when a
 * field arrives as the wrong type — the server sends `threshold` as `".25"`, a string, in the
 * sibling config response — the cast succeeds and the wrong type travels on until something
 * downstream renders `NaN` or calls a string method on a number. Reading through these turns
 * that into the declared fallback at the boundary, which is the only place with enough
 * context to choose one.
 */
const asString = (value: unknown, fallback = ''): string =>
    typeof value === 'string' ? value : fallback;

const asNumber = (value: unknown, fallback: number): number => {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
    }

    // The API is loose about this: numbers come back quoted in more than one response shape.
    const parsed = typeof value === 'string' ? Number(value) : NaN;

    return Number.isFinite(parsed) ? parsed : fallback;
};

const asRecords = (value: unknown): Record<string, unknown>[] =>
    Array.isArray(value)
        ? value.filter(
              (entry): entry is Record<string, unknown> => !!entry && typeof entry === 'object'
          )
        : [];

/**
 * Semantic search over the embeddings indexes.
 *
 * Uses `POST` with a `CompletionsForm` body. The published SDK's `AISearch` issues `GET` with
 * query params against the same resource and has its own types in `@dotcms/types`; those are
 * deliberately not reused here, because the request side is not shared and an SDK-facing
 * change should not be able to break an internal admin screen.
 */
@Injectable({ providedIn: 'root' })
export class DotAiSearchService {
    #http: HttpClient = inject(HttpClient);

    /**
     * Runs a semantic search and returns typed results.
     *
     * Each `dotCMSResults` entry is a full contentlet JSON. One server fallback path omits
     * `modDate` entirely, so it stays optional all the way to the template rather than being
     * defaulted to something untrue.
     */
    semanticSearch(form: DotAiCompletionsForm): Observable<DotAiSearchResponse> {
        return this.#http
            .post<
                Record<string, unknown>
            >(`${AI_API_ENDPOINT}/search`, JSON.stringify({ ...form, stream: false }), { headers })
            .pipe(
                map((raw) => this.#toSearchResponse(raw)),
                catchError((error: HttpErrorResponse) => throwError(() => this.#toError(error)))
            );
    }

    #toSearchResponse(raw: Record<string, unknown>): DotAiSearchResponse {
        const rows = asRecords(raw?.['dotCMSResults']);

        return {
            timeToEmbeddings: asString(raw?.['timeToEmbeddings']),
            total: asNumber(raw?.['total'], rows.length),
            count: asNumber(raw?.['count'], rows.length),
            query: asString(raw?.['query']),
            threshold: asNumber(raw?.['threshold'], 0),
            operator: asString(raw?.['operator']),
            offset: asNumber(raw?.['offset'], 0),
            limit: asNumber(raw?.['limit'], 0),
            results: rows.map((row) => this.#toResult(row))
        };
    }

    #toResult(row: Record<string, unknown>): DotAiSearchResult {
        const modDate = row?.['modDate'];

        return {
            identifier: asString(row?.['identifier']),
            inode: asString(row?.['inode']),
            title: asString(row?.['title']),
            contentType: asString(row?.['contentTypeName']) || asString(row?.['contentType']),
            // Left undefined on purpose when absent — the row drops the date and its separator.
            modDate: typeof modDate === 'string' ? modDate : undefined,
            matches: asRecords(row?.['matches']).map((match) => ({
                // 0 is a real distance (an exact match), so it cannot double as "absent".
                // A non-numeric one is dropped to 0 rather than left to render as NaN%.
                distance: asNumber(match['distance'], 0),
                extractedText: asString(match['extractedText'])
            }))
        };
    }

    #toError(error: HttpErrorResponse): DotAiIndexNotFoundError | HttpErrorResponse {
        const message = error?.error?.error;
        const match =
            error?.status === 404 && typeof message === 'string' && INDEX_NOT_FOUND.exec(message);

        return match ? { indexNotFound: true, indexName: match[1], original: error } : error;
    }
}
