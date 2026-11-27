import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
    TestRequest
} from '@angular/common/http/testing';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

import {
    DotContentDriveExportRequest,
    DotContentDriveExportResult,
    DotContentDriveExportService
} from './dot-content-drive-export.service';

const EXPORT_URL = '/api/v1/drive/export';

/** The listing request as the store builds it, filtered to one content type. */
const QUERY: DotContentDriveSearchRequest = {
    assetPath: '//demo.dotcms.com/',
    browseScope: 'ALL',
    includeSystemHost: true,
    contentTypes: ['Blog'],
    language: ['1'],
    filters: { text: '', filterFolders: true },
    contentCursor: 0,
    folderCursor: 0,
    maxResults: 40,
    sortBy: 'modDate:desc',
    showFolders: false
} as DotContentDriveSearchRequest;

const BODY: DotContentDriveExportRequest = { query: QUERY };

/** The headers a successful filter-mode export carries, per contracts/drive-export-api.md. */
const FILE_HEADERS = {
    'Content-Type': 'text/csv; charset=UTF-8',
    'Content-Disposition':
        'attachment; filename="Blog_contents_11_26_2026.csv"; filename*=UTF-8\'\'Blog_contents_11_26_2026.csv',
    'X-Export-Contentlets': '3',
    'X-Export-Files': '1'
};

/**
 * What the export rejects with when the error body carries no message it can read. It is the
 * same sentence the server sends for an unexpected failure, so the user reads one wording for
 * "something went wrong" whichever side noticed it.
 */
const GENERIC_EXPORT_ERROR =
    'The export failed. Try again, and contact your administrator if it keeps failing.';

/** An error body as the server writes it (contracts/drive-export-api.md), carrying `message`. */
const errorBody = (errorCode: string, message: string): Blob =>
    new Blob(
        [
            JSON.stringify({
                errors: [{ errorCode, message, fieldName: null }],
                entity: null
            })
        ],
        { type: 'application/json' }
    );

describe('DotContentDriveExportService', () => {
    let spectator: SpectatorService<DotContentDriveExportService>;
    let httpMock: HttpTestingController;

    const createService = createServiceFactory({
        service: DotContentDriveExportService,
        providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    beforeEach(() => {
        spectator = createService();
        httpMock = spectator.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
        vi.restoreAllMocks();
    });

    /** Runs an export, answers it with the given headers, and returns what the service emitted. */
    const exportWith = (
        headers: Record<string, string>
    ): DotContentDriveExportResult | undefined => {
        let result: DotContentDriveExportResult | undefined;
        spectator.service.export(BODY).subscribe((value) => (result = value));

        httpMock
            .expectOne(EXPORT_URL)
            .flush(new Blob(['Identifier,languageCode,countryCode,title\n']), {
                status: 200,
                statusText: 'OK',
                headers
            });

        return result;
    };

    describe('the request', () => {
        it('POSTs the listing query to /api/v1/drive/export', () => {
            spectator.service.export(BODY).subscribe();

            const req = httpMock.expectOne(EXPORT_URL);
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual({ query: QUERY });
            req.flush(new Blob(['x']), { status: 200, statusText: 'OK', headers: FILE_HEADERS });
        });

        it('asks for the whole response as a blob, so the headers can be read', () => {
            // `observe` never reaches HttpRequest, so it is read off the call itself. The spy calls
            // through, so the request still goes to the testing backend.
            const postSpy = vi.spyOn(spectator.inject(HttpClient), 'post');

            spectator.service.export(BODY).subscribe();

            expect(postSpy).toHaveBeenCalledWith(
                EXPORT_URL,
                { query: QUERY },
                expect.objectContaining({ observe: 'response', responseType: 'blob' })
            );

            const req = httpMock.expectOne(EXPORT_URL);
            expect(req.request.responseType).toBe('blob');
            req.flush(new Blob(['x']), { status: 200, statusText: 'OK', headers: FILE_HEADERS });
        });
    });

    describe('the result', () => {
        it('emits { kind: "file", blob, fileName, contentlets, files }', () => {
            const result = exportWith(FILE_HEADERS);

            expect(result).toEqual({
                kind: 'file',
                blob: expect.any(Blob),
                fileName: 'Blog_contents_11_26_2026.csv',
                contentlets: 3,
                files: 1
            });
        });

        it('takes the file name from filename* and percent-decodes it', () => {
            const result = exportWith({
                ...FILE_HEADERS,
                'Content-Disposition':
                    'attachment; filename="Art_culo_contents_11_26_2026.csv"; filename*=UTF-8\'\'Art%C3%ADculo_contents_11_26_2026.csv'
            });

            expect(result).toMatchObject({
                kind: 'file',
                fileName: 'Artículo_contents_11_26_2026.csv'
            });
        });

        it('falls back to filename, without its quotes, when filename* is absent', () => {
            const result = exportWith({
                ...FILE_HEADERS,
                'Content-Disposition': 'attachment; filename="News_contents_11_26_2026.csv"'
            });

            expect(result).toMatchObject({
                kind: 'file',
                fileName: 'News_contents_11_26_2026.csv'
            });
        });

        it('reads the X-Export-* headers as numbers', () => {
            const result = exportWith({
                ...FILE_HEADERS,
                'X-Export-Contentlets': '2500',
                'X-Export-Files': '4'
            });

            // Strict equality on numbers: the raw header strings '2500' and '4' do not match.
            expect(result).toMatchObject({ kind: 'file', contentlets: 2500, files: 4 });
        });

        it('emits { kind: "empty" } for a 204', async () => {
            const result = firstValueFrom(spectator.service.export(BODY));

            httpMock.expectOne(EXPORT_URL).flush(null, { status: 204, statusText: 'No Content' });

            expect(await result).toEqual({ kind: 'empty' });
        });
    });

    describe('a failed export', () => {
        /**
         * Runs an export, fails it with `fail`, and returns what the observable errored with.
         * Reading a Blob body is asynchronous, so the error is awaited rather than caught inline.
         * An export that succeeds instead resolves to `undefined`, which fails every assertion below.
         */
        const exportFailingWith = (fail: (req: TestRequest) => void): Promise<unknown> => {
            const result = firstValueFrom(spectator.service.export(BODY));

            fail(httpMock.expectOne(EXPORT_URL));

            return result.then(
                () => undefined,
                (error: unknown) => error
            );
        };

        it.each([
            { status: 400, statusText: 'Bad Request', errorCode: 'BAD_REQUEST' },
            { status: 404, statusText: 'Not Found', errorCode: 'NOT_FOUND' },
            { status: 500, statusText: 'Internal Server Error', errorCode: 'INTERNAL_SERVER_ERROR' }
        ])(
            'errors with the server message from the Blob body of a $status',
            async ({ status, statusText, errorCode }) => {
                const message = `The server refused the export (${status})`;

                const error = await exportFailingWith((req) =>
                    req.flush(errorBody(errorCode, message), { status, statusText })
                );

                expect(error).toBeInstanceOf(Error);
                expect((error as Error).message).toBe(message);
            }
        );

        it.each([
            {
                body: 'a Blob body that is not JSON',
                fail: (req: TestRequest) =>
                    req.flush(new Blob(['<html>Bad Gateway</html>'], { type: 'text/html' }), {
                        status: 500,
                        statusText: 'Internal Server Error'
                    })
            },
            {
                body: 'a JSON body with no errors array',
                fail: (req: TestRequest) =>
                    req.flush(
                        new Blob([JSON.stringify({ entity: null })], {
                            type: 'application/json'
                        }),
                        { status: 400, statusText: 'Bad Request' }
                    )
            },
            {
                body: 'a JSON body whose errors array is empty',
                fail: (req: TestRequest) =>
                    req.flush(
                        new Blob([JSON.stringify({ errors: [], entity: null })], {
                            type: 'application/json'
                        }),
                        { status: 404, statusText: 'Not Found' }
                    )
            },
            {
                body: 'no body at all',
                fail: (req: TestRequest) =>
                    req.flush(null, { status: 500, statusText: 'Internal Server Error' })
            },
            {
                body: 'a network error (no response, status 0)',
                fail: (req: TestRequest) => req.error(new ProgressEvent('error'))
            }
        ])(`errors with "${GENERIC_EXPORT_ERROR}" for $body`, async ({ fail }) => {
            const error = await exportFailingWith(fail);

            expect(error).toBeInstanceOf(Error);
            expect((error as Error).message).toBe(GENERIC_EXPORT_ERROR);
        });
    });
});
