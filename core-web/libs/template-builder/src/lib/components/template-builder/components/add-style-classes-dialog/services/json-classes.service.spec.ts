import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { JsonClassesService, STYLE_CLASSES_FILE_URL } from './json-classes.service';

import { MOCK_STYLE_CLASSES_FILE } from '../../../utils/mocks';

describe('JsonClassesService', () => {
    let spectator: SpectatorHttp<JsonClassesService>;
    const createHttp = createHttpFactory(JsonClassesService);

    beforeEach(() => (spectator = createHttp()));

    it('should be requested to the expected URL', () => {
        spectator.service.getClasses().subscribe();
        spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);
    });

    it('should return all classes', () => {
        spectator.service.getClasses().subscribe();

        const req = spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);
        expect(req.request.body).toEqual(MOCK_STYLE_CLASSES_FILE.classes);

        req.flush(MOCK_STYLE_CLASSES_FILE);
    });

    it('should return an empty array with a error', () => {
        spectator.service.getClasses().subscribe();

        const req = spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);
        expect(req.request.body).toEqual([]);

        req.flush('', { status: 404 });
    });
});
