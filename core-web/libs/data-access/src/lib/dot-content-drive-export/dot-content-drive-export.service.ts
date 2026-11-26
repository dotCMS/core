import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

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
    /**
     * Asks the server to build the export file for the given request.
     *
     * Not implemented yet (compile-only stub for the TDD Red step): it completes without a value
     * and sends no request.
     *
     * @param _body The export request, carrying the listing query in filter mode.
     * @returns The file and its counts once the server has built it.
     */
    export(_body: DotContentDriveExportRequest): Observable<DotContentDriveExportResult> {
        return EMPTY;
    }
}
