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
        const rows = (raw?.['dotCMSResults'] as Record<string, unknown>[]) ?? [];

        return {
            timeToEmbeddings: (raw?.['timeToEmbeddings'] as string) ?? '',
            total: (raw?.['total'] as number) ?? rows.length,
            count: (raw?.['count'] as number) ?? rows.length,
            query: (raw?.['query'] as string) ?? '',
            threshold: (raw?.['threshold'] as number) ?? 0,
            operator: (raw?.['operator'] as string) ?? '',
            offset: (raw?.['offset'] as number) ?? 0,
            limit: (raw?.['limit'] as number) ?? 0,
            results: rows.map((row) => this.#toResult(row))
        };
    }

    #toResult(row: Record<string, unknown>): DotAiSearchResult {
        const matches = (row?.['matches'] as { distance: number; extractedText: string }[]) ?? [];

        return {
            identifier: (row?.['identifier'] as string) ?? '',
            inode: (row?.['inode'] as string) ?? '',
            title: (row?.['title'] as string) ?? '',
            contentType:
                (row?.['contentTypeName'] as string) ?? (row?.['contentType'] as string) ?? '',
            // Left undefined on purpose when absent — the row drops the date and its separator.
            modDate: (row?.['modDate'] as string) ?? undefined,
            matches: matches.map((match) => ({
                distance: match.distance,
                extractedText: match.extractedText
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
