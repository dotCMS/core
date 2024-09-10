import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';

import { SuggestionsService } from './suggestions.service';

describe('SuggestionsService', () => {
    let spectator: SpectatorHttp<SuggestionsService>;
    const createHttp = createHttpFactory(SuggestionsService);

    beforeEach(() => (spectator = createHttp()));

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });
});
