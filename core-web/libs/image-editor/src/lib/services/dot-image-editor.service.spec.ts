import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotImageEditorService } from './dot-image-editor.service';

describe('DotImageEditorService', () => {
    let spectator: SpectatorService<DotImageEditorService>;
    let httpMock: HttpTestingController;
    let httpErrorManager: DotHttpErrorManagerService;

    const createService = createServiceFactory({
        service: DotImageEditorService,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        httpMock = spectator.inject(HttpTestingController);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('getFileSize', () => {
        it('should issue a HEAD request and parse Content-Length to a number', () => {
            const result: number[] = [];
            spectator.service.getFileSize('/dA/asset.png').subscribe((size) => result.push(size));

            const req = httpMock.expectOne('/dA/asset.png');
            expect(req.request.method).toBe('HEAD');
            req.flush(null, { headers: { 'Content-Length': '2048' } });

            expect(result).toEqual([2048]);
        });

        it('should return 0 when Content-Length header is missing', () => {
            const result: number[] = [];
            spectator.service.getFileSize('/dA/asset.png').subscribe((size) => result.push(size));

            httpMock.expectOne('/dA/asset.png').flush(null);

            expect(result).toEqual([0]);
        });

        it('should return 0 on HTTP error without throwing', () => {
            const result: number[] = [];
            let errored = false;
            spectator.service.getFileSize('/dA/asset.png').subscribe({
                next: (size) => result.push(size),
                error: () => (errored = true)
            });

            httpMock
                .expectOne('/dA/asset.png')
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(result).toEqual([0]);
            expect(errored).toBe(false);
        });
    });

    describe('saveEditedImage', () => {
        const tempResponse = {
            id: 'temp_123',
            fileName: 'edited.png',
            length: 4096,
            metadata: { contentType: 'image/png' }
        };

        it('should GET a URL with binaryFieldId and _imageToolSaveFile and map to a temp file', () => {
            const result: unknown[] = [];
            spectator.service
                .saveEditedImage('/dA/asset.png/filter/Grayscale/grayscale/1', 'fileField')
                .subscribe((file) => result.push(file));

            const req = httpMock.expectOne((request) => request.url.startsWith('/dA/asset.png'));
            expect(req.request.method).toBe('GET');
            expect(req.request.urlWithParams).toContain('binaryFieldId=fileField');
            expect(req.request.urlWithParams).toContain('_imageToolSaveFile=true');
            req.flush(tempResponse);

            expect(result[0]).toEqual({
                id: 'temp_123',
                fileName: 'edited.png',
                length: 4096,
                metadata: { contentType: 'image/png' },
                folder: '',
                image: true,
                mimeType: 'image/png',
                referenceUrl: '',
                thumbnailUrl: ''
            });
        });

        it('should append the save tokens with & when the filter URL already has a query', () => {
            spectator.service.saveEditedImage('/dA/asset.png?test=1', 'fileField').subscribe();

            const req = httpMock.expectOne((request) => request.url.startsWith('/dA/asset.png'));
            expect(req.request.urlWithParams).toContain('test=1');
            expect(req.request.urlWithParams).toContain('&binaryFieldId=fileField');
            req.flush(tempResponse);
        });

        it('should handle the error and rethrow on failure', () => {
            let errored = false;
            spectator.service.saveEditedImage('/dA/asset.png', 'fileField').subscribe({
                error: () => (errored = true)
            });

            httpMock
                .expectOne((request) => request.url.startsWith('/dA/asset.png'))
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(errored).toBe(true);
        });
    });

    describe('persistFocalPoint', () => {
        it('should GET the FocalPoint filter URL with an overwrite cache-buster', () => {
            spectator.service.persistFocalPoint('/dA/asset.png', { x: 0.25, y: 0.75 }).subscribe();

            const req = httpMock.expectOne((request) => request.url.startsWith('/dA/asset.png'));
            expect(req.request.method).toBe('GET');
            expect(req.request.urlWithParams).toContain('/filter/FocalPoint/fp/0.25,0.75/');
            expect(req.request.urlWithParams).toContain('overwrite=');
            req.flush('');
        });

        it('should complete with void on HTTP error without throwing', () => {
            let completed = false;
            let errored = false;
            spectator.service.persistFocalPoint('/dA/asset.png', { x: 0.5, y: 0.5 }).subscribe({
                complete: () => (completed = true),
                error: () => (errored = true)
            });

            httpMock
                .expectOne((request) => request.url.startsWith('/dA/asset.png'))
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(completed).toBe(true);
            expect(errored).toBe(false);
        });
    });

    describe('triggerDownload', () => {
        it('should create an anchor with href/download and click it', () => {
            const anchor = document.createElement('a');
            const clickSpy = jest.spyOn(anchor, 'click').mockImplementation(jest.fn());
            const createSpy = jest
                .spyOn(document, 'createElement')
                .mockReturnValue(anchor as HTMLAnchorElement);

            spectator.service.triggerDownload('/dA/asset.png', 'edited.png');

            expect(createSpy).toHaveBeenCalledWith('a');
            expect(anchor.getAttribute('href')).toBe('/dA/asset.png');
            expect(anchor.download).toBe('edited.png');
            expect(clickSpy).toHaveBeenCalled();

            createSpy.mockRestore();
        });
    });
});
