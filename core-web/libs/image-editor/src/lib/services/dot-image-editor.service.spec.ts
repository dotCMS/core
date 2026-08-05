import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DotImageEditorService } from './dot-image-editor.service';

describe('DotImageEditorService', () => {
    let spectator: SpectatorService<DotImageEditorService>;
    let httpMock: HttpTestingController;

    const createService = createServiceFactory({
        service: DotImageEditorService,
        providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    // jsdom doesn't implement URL.createObjectURL (it's `undefined`), so it can't be
    // spied — capture the original and restore it after each test instead, so the global
    // isn't left patched for other spec files sharing the Jest worker.
    const originalCreateObjectURL = URL.createObjectURL;

    beforeEach(() => {
        URL.createObjectURL = jest.fn(() => 'blob:mock-object-url');
        spectator = createService();
        httpMock = spectator.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
        URL.createObjectURL = originalCreateObjectURL;
        jest.restoreAllMocks();
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

        it('should return null when Content-Length header is missing', () => {
            const result: (number | null)[] = [];
            spectator.service.getFileSize('/dA/asset.png').subscribe((size) => result.push(size));

            httpMock.expectOne('/dA/asset.png').flush(null);

            // null (not 0) so an unknown size renders as "—" rather than "0.0 KB".
            expect(result).toEqual([null]);
        });

        it('should return null on HTTP error without throwing', () => {
            const result: (number | null)[] = [];
            let errored = false;
            spectator.service.getFileSize('/dA/asset.png').subscribe({
                next: (size) => result.push(size),
                error: () => (errored = true)
            });

            httpMock
                .expectOne('/dA/asset.png')
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(result).toEqual([null]);
            expect(errored).toBe(false);
        });
    });

    describe('loadPreviewImage', () => {
        it('should GET the URL as a blob and return an object URL for a complete image', () => {
            const blob = new Blob(['imagedata'], { type: 'image/png' });
            let result: string | undefined;
            spectator.service
                .loadPreviewImage('/dA/preview.png')
                .subscribe((url) => (result = url));

            const req = httpMock.expectOne('/dA/preview.png');
            expect(req.request.method).toBe('GET');
            expect(req.request.responseType).toBe('blob');
            req.flush(blob, {
                headers: { 'Content-Length': String(blob.size), 'Content-Type': 'image/png' }
            });

            expect(URL.createObjectURL).toHaveBeenCalledWith(blob);
            expect(result).toBe('blob:mock-object-url');
        });

        it('should accept an image blob when no Content-Length is present', () => {
            const blob = new Blob(['x'], { type: 'image/jpeg' });
            let result: string | undefined;
            spectator.service
                .loadPreviewImage('/dA/preview.png')
                .subscribe((url) => (result = url));

            httpMock
                .expectOne('/dA/preview.png')
                .flush(blob, { headers: { 'Content-Type': 'image/jpeg' } });

            expect(result).toBe('blob:mock-object-url');
        });

        it('should error when the body is shorter than the declared Content-Length (truncated)', () => {
            const blob = new Blob(['x'], { type: 'image/png' });
            let errored = false;
            spectator.service.loadPreviewImage('/dA/preview.png').subscribe({
                next: () => undefined,
                error: () => (errored = true)
            });

            httpMock.expectOne('/dA/preview.png').flush(blob, {
                headers: { 'Content-Length': '500', 'Content-Type': 'image/png' }
            });

            expect(errored).toBe(true);
        });

        it('should error on an empty body', () => {
            const blob = new Blob([], { type: 'image/png' });
            let errored = false;
            spectator.service.loadPreviewImage('/dA/preview.png').subscribe({
                next: () => undefined,
                error: () => (errored = true)
            });

            httpMock
                .expectOne('/dA/preview.png')
                .flush(blob, { headers: { 'Content-Type': 'image/png' } });

            expect(errored).toBe(true);
        });

        it('should reject an HTML/JSON error body served with a 200', () => {
            const blob = new Blob(['<html>error</html>'], { type: 'text/html' });
            let errored = false;
            spectator.service.loadPreviewImage('/dA/preview.png').subscribe({
                next: () => undefined,
                error: () => (errored = true)
            });

            httpMock
                .expectOne('/dA/preview.png')
                .flush(blob, { headers: { 'Content-Type': 'text/html' } });

            expect(errored).toBe(true);
        });
    });

    describe('triggerDownload', () => {
        it('should create an anchor with href/download and click it', () => {
            const anchor = document.createElement('a');
            const clickSpy = jest.spyOn(anchor, 'click').mockImplementation(jest.fn());
            const createSpy = jest
                .spyOn(document, 'createElement')
                .mockReturnValue(anchor as HTMLAnchorElement);
            const appendSpy = jest.spyOn(document.body, 'appendChild');
            const removeSpy = jest.spyOn(anchor, 'remove');

            spectator.service.triggerDownload('/dA/asset.png', 'edited.png');

            expect(createSpy).toHaveBeenCalledWith('a');
            expect(anchor.getAttribute('href')).toBe('/dA/asset.png');
            expect(anchor.download).toBe('edited.png');
            // The anchor must be attached, clicked, then detached so the click works
            // cross-browser and leaves no orphan node behind.
            expect(appendSpy).toHaveBeenCalledWith(anchor);
            expect(clickSpy).toHaveBeenCalled();
            expect(removeSpy).toHaveBeenCalled();

            createSpy.mockRestore();
            appendSpy.mockRestore();
        });
    });
});
