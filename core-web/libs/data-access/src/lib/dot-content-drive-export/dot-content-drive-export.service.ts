import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

/** The endpoint that builds the export file. */
const EXPORT_URL = '/api/v1/drive/export';

/**
 * Body of `POST /api/v1/drive/export` in filter mode: the same request the listing sends to
 * `/api/v1/drive/search`. The server ignores its paging, sorting and folder flags.
 */
export interface DotContentDriveExportRequest {
    query: DotContentDriveSearchRequest;
}

/**
 * A 200 from the export endpoint: the file and what the server says it holds.
 *
 * `fileName` comes from `Content-Disposition` (`filename*` decoded when present, else `filename`);
 * `contentlets` and `files` come from the `X-Export-Contentlets` and `X-Export-Files` headers.
 */
export interface DotContentDriveExportFile {
    kind: 'file';
    blob: Blob;
    fileName: string;
    contentlets: number;
    files: number;
}

/** A 204 from the export endpoint: nothing readable matched, so there is no file. */
export interface DotContentDriveExportEmpty {
    kind: 'empty';
}

/** What an export resolves to. */
export type DotContentDriveExportResult = DotContentDriveExportFile | DotContentDriveExportEmpty;

/**
 * Downloads Content Drive exports: one CSV for the content type the listing is filtered to.
 */
@Injectable({
    providedIn: 'root'
})
export class DotContentDriveExportService {
    readonly #http = inject(HttpClient);

    /**
     * Asks the server to build the export file for the given request.
     *
     * The whole response is read as a blob, so the file name and the counts can be taken from its
     * headers. A 204 (nothing readable matched) resolves to `{ kind: 'empty' }`.
     *
     * @param body The export request, carrying the listing query in filter mode.
     * @returns The file and its counts once the server has built it, or `empty` when there is none.
     */
    export(body: DotContentDriveExportRequest): Observable<DotContentDriveExportResult> {
        return this.#http
            .post(EXPORT_URL, body, { observe: 'response', responseType: 'blob' })
            .pipe(map((response) => toExportResult(response)));
    }
}

/**
 * Turns the HTTP response into an export result: `empty` for a 204 or a missing body, otherwise
 * the file with the name and counts its headers carry.
 */
function toExportResult(response: HttpResponse<Blob>): DotContentDriveExportResult {
    if (response.status === 204 || !response.body) {
        return { kind: 'empty' };
    }

    const headers = response.headers;

    return {
        kind: 'file',
        blob: response.body,
        fileName: parseFileName(headers.get('Content-Disposition')),
        contentlets: readCount(headers, 'X-Export-Contentlets'),
        files: readCount(headers, 'X-Export-Files')
    };
}

/**
 * Reads the file name out of a `Content-Disposition` header.
 *
 * The RFC 5987 `filename*=UTF-8''…` parameter wins, percent-decoded, because it carries names
 * that are not ASCII; otherwise the plain `filename` is used without its quotes. Returns an empty
 * string when the header is missing or names no file, which `getDownloadLink` still accepts.
 */
function parseFileName(header: string | null): string {
    if (!header) {
        return '';
    }

    const extended = header.match(/filename\*\s*=\s*(?:[\w-]+)?'[^']*'([^;]+)/i);
    if (extended) {
        try {
            return decodeURIComponent(extended[1].trim());
        } catch {
            // A malformed escape: fall back to the plain parameter below.
        }
    }

    const plain = header.match(/filename\s*=\s*("([^"]*)"|[^;]+)/i);

    return plain ? (plain[2] ?? plain[1]).trim() : '';
}

/** Reads a count header as a number; a missing or unparseable header counts as zero. */
function readCount(headers: HttpHeaders, name: string): number {
    const value = Number(headers.get(name));

    return Number.isFinite(value) ? value : 0;
}
