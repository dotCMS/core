import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DOT_AUTH_TOKEN, DOT_BASE_URL } from './dot.config';

export interface DotContentlet {
    inode: string;
    identifier: string;
    title: string;
    contentType: string;
    modDate: string;
    languageId: number;
    [key: string]: unknown;
}

/** One page from POST /api/content/_search (for paginated UI). */
export interface DotContentSearchPage {
    contentlets: DotContentlet[];
    totalRecords: number;
}

/** POST /api/content/_search wraps results in ResponseEntityView → SearchView. */
interface ContentSearchResponse {
    entity?: {
        contentTook?: number;
        jsonObjectView?: { contentlets?: DotContentlet[] };
        queryTook?: number;
        resultsSize?: number;
    };
}

const imageSearchQuery = (languageId: number) =>
    `+catchall:* title:''^15 +languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`;

const videoSearchQuery = (languageId: number) =>
    `+catchall:* title:''^15 +languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:video/* +deleted:false +working:true`;

@Injectable({ providedIn: 'root' })
export class DotContentletService {
    private readonly http = inject(HttpClient);

    /**
     * Search published image assets via POST /api/content/_search.
     * @param text Optional filter; when empty, uses the default broad image query.
     */
    searchImages(
        params: { text?: string; offset?: number; limit?: number; languageId?: number } = {}
    ): Observable<DotContentSearchPage> {
        const limit = params.limit ?? 20;
        const offset = params.offset ?? 0;
        const languageId = params.languageId ?? 1;
        const raw = params.text?.trim() ?? '';
        const query = raw
            ? DotContentletService.buildFilteredImageQuery(raw, languageId)
            : imageSearchQuery(languageId);

        return this.postContentSearch(query, limit, offset);
    }

    /**
     * Search published video assets via POST /api/content/_search.
     * @param text Optional filter; when empty, uses the default broad video query.
     */
    searchVideos(
        params: { text?: string; offset?: number; limit?: number; languageId?: number } = {}
    ): Observable<DotContentSearchPage> {
        const limit = params.limit ?? 20;
        const offset = params.offset ?? 0;
        const languageId = params.languageId ?? 1;
        const raw = params.text?.trim() ?? '';
        const query = raw
            ? DotContentletService.buildFilteredVideoQuery(raw, languageId)
            : videoSearchQuery(languageId);

        return this.postContentSearch(query, limit, offset);
    }

    private postContentSearch(
        query: string,
        limit: number,
        offset: number
    ): Observable<DotContentSearchPage> {
        const headers = new HttpHeaders({
            Authorization: `Bearer ${DOT_AUTH_TOKEN}`,
            'Content-Type': 'application/json'
        });

        return this.http
            .post<ContentSearchResponse>(
                `${DOT_BASE_URL}/api/content/_search`,
                {
                    query,
                    sort: 'score,modDate desc',
                    limit,
                    offset
                },
                { headers }
            )
            .pipe(
                map((res) => {
                    const contentlets = res.entity?.jsonObjectView?.contentlets ?? [];
                    const reported = res.entity?.resultsSize;
                    let totalRecords: number;
                    if (typeof reported === 'number' && !Number.isNaN(reported)) {
                        totalRecords = reported;
                    } else if (contentlets.length < limit) {
                        // Last (or only) page — exact count when API omits resultsSize
                        totalRecords = offset + contentlets.length;
                    } else {
                        // Full page but no total from API — assume at least one more row so paginator appears
                        totalRecords = offset + contentlets.length + 1;
                    }
                    return { contentlets, totalRecords };
                })
            );
    }

    private static escapeLuceneToken(term: string): string {
        const specials = '+-&|!(){}[]^"~*?:\\';
        let out = '';
        for (const ch of term) {
            out += specials.includes(ch) ? `\\${ch}` : ch;
        }
        return out;
    }

    /** Narrow results with one or more whitespace-separated tokens (each as +catchall:token*). */
    private static buildFilteredImageQuery(text: string, languageId: number): string {
        return DotContentletService.buildFilteredAssetQuery(
            text,
            `+languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`
        );
    }

    private static buildFilteredVideoQuery(text: string, languageId: number): string {
        return DotContentletService.buildFilteredAssetQuery(
            text,
            `+languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:video/* +deleted:false +working:true`
        );
    }

    private static buildFilteredAssetQuery(text: string, base: string): string {
        const tokens = text.trim().split(/\s+/).filter(Boolean);
        const catchalls = tokens
            .map((t) => `+catchall:${DotContentletService.escapeLuceneToken(t)}*`)
            .join(' ');
        return `${catchalls} ${base}`;
    }

    fetchByType(variable: string, languageId = 1): Observable<DotContentlet[]> {
        const headers = new HttpHeaders({
            Authorization: `Bearer ${DOT_AUTH_TOKEN}`,
            'Content-Type': 'application/json'
        });

        return this.http
            .post<ContentSearchResponse>(
                `${DOT_BASE_URL}/api/content/_search`,
                {
                    query: `+contentType:${variable} +languageId:${languageId} +deleted:false +working:true +catchall:** title:''^15`,
                    sort: 'modDate desc',
                    offset: 0,
                    limit: 40
                },
                { headers }
            )
            .pipe(map((res) => res.entity?.jsonObjectView?.contentlets ?? []));
    }
}
