import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotAiEmbeddingsBuildForm,
    DotAiEmbeddingsBuildResult,
    DotAiIndex
} from '@dotcms/dotcms-models';

import { AI_API_ENDPOINT } from './dot-ai.constants';

/** Raw per-index row as the backend returns it, before any conversion. */
interface RawIndexCount {
    fragments: number;
    contents: number;
    tokenTotal: number;
    tokensPerChunk: number;
    /** Comma-joined at the SQL level via STRING_AGG. */
    contentTypes: string | null;
}

const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

/**
 * Embeddings index administration.
 *
 * This service owns three wire→view conversions, and is the only place any of them happens:
 * the `indexCount` wrapper map, the `contentTypes` CSV, and the `{deleted:N}` /
 * `{created:true}` envelopes. Stores and components never see a map, a CSV or an envelope.
 *
 * Note `getIndexes` and `rebuildEmbeddingsDb` require CMS_ADMINISTRATOR_ROLE server-side; a
 * 403 here is a normal state for a non-admin, not an error to surface as a dialog.
 */
@Injectable({ providedIn: 'root' })
export class DotAiEmbeddingsService {
    #http: HttpClient = inject(HttpClient);

    readonly #endpoint = `${AI_API_ENDPOINT}/embeddings`;

    /**
     * Every index with its counts and coverage.
     *
     * The response is wrapped one level (`{indexCount: {...}}`) and keyed by index name, so
     * the key is folded into a `name` field and the whole thing becomes an array. The backend
     * returns a TreeMap, so the order is already alphabetical.
     */
    getIndexes(): Observable<DotAiIndex[]> {
        return this.#http
            .get<{ indexCount: Record<string, RawIndexCount> }>(`${this.#endpoint}/indexCount`)
            .pipe(
                map((response) =>
                    Object.entries(response?.indexCount ?? {}).map(([name, raw]) => ({
                        name,
                        fragments: raw.fragments,
                        contents: raw.contents,
                        tokenTotal: raw.tokenTotal,
                        tokensPerChunk: raw.tokensPerChunk,
                        contentTypes: (raw.contentTypes ?? '')
                            .split(',')
                            .map((type) => type.trim())
                            .filter(Boolean)
                    }))
                )
            );
    }

    /**
     * Builds (or adds to) an index from a content query.
     *
     * Takes an `EmbeddingsForm`, **not** a `CompletionsForm` — a different shape from the
     * retrieval payload Search and Chat share, which is why the build dialog assembles its
     * own body.
     */
    buildIndex(form: DotAiEmbeddingsBuildForm): Observable<DotAiEmbeddingsBuildResult> {
        return this.#http.post<DotAiEmbeddingsBuildResult>(this.#endpoint, JSON.stringify(form), {
            headers
        });
    }

    /** Deletes a whole index. Returns how many rows went. */
    deleteIndex(indexName: string): Observable<number> {
        return this.#deleteWithBody({ indexName });
    }

    /** Deletes only the content matching `deleteQuery` from an index. */
    deleteFromIndex(indexName: string, deleteQuery: string): Observable<number> {
        return this.#deleteWithBody({ indexName, deleteQuery });
    }

    /** Drops and recreates the embeddings store. Destructive — always confirm first. */
    rebuildEmbeddingsDb(): Observable<boolean> {
        return this.#http
            .delete<{ created: boolean }>(`${this.#endpoint}/db`, { headers })
            .pipe(map((response) => !!response?.created));
    }

    /**
     * Angular's `delete` only sends a body when one is passed in the options object, and a
     * silently dropped body here would delete nothing while still answering 200.
     */
    #deleteWithBody(body: Record<string, string>): Observable<number> {
        return this.#http
            .delete<{ deleted: number }>(this.#endpoint, { headers, body: JSON.stringify(body) })
            .pipe(map((response) => response?.deleted ?? 0));
    }
}
