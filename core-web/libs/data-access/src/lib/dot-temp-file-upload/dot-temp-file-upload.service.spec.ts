import { expect, it, describe } from '@jest/globals';
import { of } from 'rxjs';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotTempFileUploadService } from './dot-temp-file-upload.service';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';

describe('DotTempFileUploadService', () => {
    let service: DotTempFileUploadService;
    let httpMock: HttpTestingController;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotTempFileUploadService,
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jest.fn().mockReturnValue(
                            of({
                                status: {
                                    toString: () => ''
                                }
                            })
                        )
                    }
                }
            ]
        });
        service = TestBed.inject(DotTempFileUploadService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should upload a file by url', (done) => {
        service.upload('https://dotcms.com/image.jpg').subscribe((res) => {
            expect(res).toEqual([
                {
                    fileName: 'fileName',
                    folder: 'folder',
                    id: 'id',
                    image: true,
                    length: 10,
                    mimeType: 'mimeType',
                    referenceUrl: 'referenceUrl',
                    thumbnailUrl: 'thumbnailUrl'
                }
            ]);
            done();
        });

        const req = httpMock.expectOne('/api/v1/temp/byUrl');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            remoteUrl: 'https://dotcms.com/image.jpg'
        });

        req.flush({
            tempFiles: [
                {
                    fileName: 'fileName',
                    folder: 'folder',
                    id: 'id',
                    image: true,
                    length: 10,
                    mimeType: 'mimeType',
                    referenceUrl: 'referenceUrl',
                    thumbnailUrl: 'thumbnailUrl'
                }
            ]
        });
    });

    it('should upload a file by file', (done) => {
        const file = new File([''], 'filename', { type: 'text/html' });

        service.upload(file).subscribe((res) => {
            expect(res).toEqual([
                {
                    fileName: 'fileName',
                    folder: 'folder',
                    id: 'id',
                    image: true,
                    length: 10,
                    mimeType: 'mimeType',
                    referenceUrl: 'referenceUrl',
                    thumbnailUrl: 'thumbnailUrl'
                }
            ]);
            done();
        });

        const req = httpMock.expectOne('/api/v1/temp');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(expect.any(FormData));

        req.flush({
            tempFiles: [
                {
                    fileName: 'fileName',
                    folder: 'folder',
                    id: 'id',
                    image: true,
                    length: 10,
                    mimeType: 'mimeType',
                    referenceUrl: 'referenceUrl',
                    thumbnailUrl: 'thumbnailUrl'
                }
            ]
        });
    });

    it('should handle error', () => {
        service.upload('https://dotcms.com/image.jpg').subscribe();

        const req = httpMock.expectOne('/api/v1/temp/byUrl');
        req.flush('deliberate 404 error', { status: 404, statusText: 'Not Found' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
            expect.any(HttpErrorResponse)
        );
    });
});
