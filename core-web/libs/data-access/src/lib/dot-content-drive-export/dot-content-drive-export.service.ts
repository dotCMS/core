import { from, Observable, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

/** The endpoint that builds the export file. */
const EXPORT_URL = '/api/v1/drive/export';

/**
 * Shown when an error carries no message the user can read: the same sentence the server sends for
 * an unexpected failure, so the user reads one wording whichever side noticed it.
 */
const GENERIC_EXPORT_ERROR =
    'The export failed. Try again, and contact your administrator if it keeps failing.';

/**
 * Body of `POST /api/v1/drive/export` in filter mode: the same request the listing sends to
 * `/api/v1/drive/search`. The server ignores its paging, sorting and folder flags.
 */
export interface DotContentDriveExportFilterRequest {
    query: DotContentDriveSearchRequest;
}

/** One selected contentlet: a language version, so the same identifier may appear once per language. */
export interface DotContentDriveExportContentlet {
    identifier: string;
    languageId: number;
}

/**
 * Body of `POST /api/v1/drive/export` in selection mode: the selected items of the site being
 * browsed. Folders are sent as `//host/path/` paths; contentlets as identifier and language.
 */
export interface DotContentDriveExportSelectionRequest {
    siteId: string;
    selection: {
        folders: string[];
        contentlets: DotContentDriveExportContentlet[];
    };
}

/** Body of `POST /api/v1/drive/export`: filter mode or selection mode. */
export type DotContentDriveExportRequest =
    | DotContentDriveExportFilterRequest
    | DotContentDriveExportSelectionRequest;

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
 * Downloads Content Drive exports: one CSV for the content type the listing is filtered to, or a
 * zip with one CSV per content type for a selection.
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
     * An error response (400, 404, 500) arrives as a blob too, so its JSON body is read to error
     * with the server's `errors[0].message`; a body with no readable message errors with a generic
     * one.
     *
     * @param body The export request: the listing query in filter mode, or the site and the
     * selected items in selection mode.
     * @returns The file and its counts once the server has built it, or `empty` when there is none.
     */
    export(body: DotContentDriveExportRequest): Observable<DotContentDriveExportResult> {
        return this.#http
            .post(EXPORT_URL, body, { observe: 'response', responseType: 'blob' })
            .pipe(
                map((response) => toExportResult(response)),
                catchError((error: unknown) =>
                    from(readErrorMessage(error)).pipe(
                        switchMap((message) => throwError(() => new Error(message)))
                    )
                )
            );
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

/**
 * Reads the message out of an export error: the `errors[0].message` of the JSON body the server
 * sends as a blob. A network error, a body that is not JSON, or JSON without a message gives the
 * generic message.
 */
async function readErrorMessage(error: unknown): Promise<string> {
    if (!(error instanceof HttpErrorResponse) || !(error.error instanceof Blob)) {
        return GENERIC_EXPORT_ERROR;
    }

    try {
        const parsed = JSON.parse(await error.error.text()) as {
            errors?: { message?: unknown }[];
        };
        const message = parsed?.errors?.[0]?.message;

        return typeof message === 'string' && message.trim() ? message : GENERIC_EXPORT_ERROR;
    } catch {
        return GENERIC_EXPORT_ERROR;
    }
}
