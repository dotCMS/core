import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';

import { DotActionUrlService } from './dot-action-url.service';

describe('DotActionUrlService', () => {
    let spectator: SpectatorService<DotActionUrlService>;
    let httpClientMock: jest.Mocked<HttpClient>;
    const createService = createServiceFactory({
        service: DotActionUrlService,
        mocks: [HttpClient]
    });

    beforeEach(() => {
        spectator = createService();
        httpClientMock = spectator.inject(HttpClient) as jest.Mocked<HttpClient>;
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

    it('should return EMPTY when the request fails', () => {
        httpClientMock.get.mockReturnValue(throwError(() => new Error('Error')));

        spectator.service.getCreateContentletUrl('testType').subscribe((result) => {
            expect(result).toEqual([]);
        });
    });
});
