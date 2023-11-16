import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotPageApiService } from './dot-page-api.service';

describe('DotPageApiService', () => {
    let spectator: SpectatorHttp<DotPageApiService>;
    const createHttp = createHttpFactory(DotPageApiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should send a GET request to retrieve page data', () => {
        // const mockResponse: DotPageApiResponse = { page: { title: 'Test Title' } };

        spectator.service.get({ url: 'test-url', language_id: 'en' }).subscribe();

        spectator.expectOne('/api/v1/page/json/test-url?language_id=en', HttpMethod.GET);
    });

    // Add more tests as needed
});
