import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mocked } from 'vitest';

import { HttpClient } from '@angular/common/http';

import { DotActionUrlService } from './dot-action-url.service';

describe('DotActionUrlService', () => {
    let spectator: SpectatorService<DotActionUrlService>;
    let httpClientMock: Mocked<HttpClient>;
    const createService = createServiceFactory({
        service: DotActionUrlService,
        mocks: [HttpClient]
    });

    beforeEach(() => {
        spectator = createService();
        httpClientMock = spectator.inject(HttpClient) as Mocked<HttpClient>;
    });

    it('should get the URL to create a contentlet', () => {
        const mockResponse = { entity: 'testUrl' };
        httpClientMock.get.mockReturnValue(of(mockResponse));

        spectator.service.getCreateContentletUrl('testType').subscribe((url) => {
            expect(url).toEqual('testUrl');
        });

        expect(httpClientMock.get).toHaveBeenCalledWith(
            '/api/v1/portlet/_actionurl/testType?language_id=1'
        );
    });

    it('should get the URL to create a contentlet with a specify language id', () => {
        const mockResponse = { entity: 'testUrl' };
        httpClientMock.get.mockReturnValue(of(mockResponse));

        spectator.service.getCreateContentletUrl('testType', 2).subscribe((url) => {
            expect(url).toEqual('testUrl');
        });

        expect(httpClientMock.get).toHaveBeenCalledWith(
            '/api/v1/portlet/_actionurl/testType?language_id=2'
        );
    });

    // The variable can come from a URL (Content Drive's `createContent`), so it is encoded into the
    // path rather than allowed to rewrite it (Constitution III, #37759 T111).
    it('should encode the content type variable into the path', () => {
        httpClientMock.get.mockReturnValue(of({ entity: 'testUrl' }));

        spectator.service.getCreateContentletUrl('a/b c', 2).subscribe();

        expect(httpClientMock.get).toHaveBeenCalledWith(
            '/api/v1/portlet/_actionurl/a%2Fb%20c?language_id=2'
        );
    });

    it('should propagate the error when the request fails', () =>
        new Promise<void>((done) => {
            const error = new Error('Not Found');
            httpClientMock.get.mockReturnValue(throwError(() => error));

            spectator.service.getCreateContentletUrl('unknownType').subscribe({
                error: (e) => {
                    expect(e).toBe(error);
                    done();
                }
            });
        }));
});
