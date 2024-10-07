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
        spectator.service.getClasses().subscribe((res) => {
            expect(res).toEqual(MOCK_STYLE_CLASSES_FILE.classes);
        });

        const req = spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);

        req.flush(MOCK_STYLE_CLASSES_FILE);
    });

    it('should return an empty array with a error', () => {
        spectator.service.getClasses().subscribe((res) => {
            expect(res).toEqual([]);
        });

        const req = spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);

        req.flush('', { status: 404, statusText: 'Not Found' });
    });

    it('should return an empty array with a bad format', () => {
        spectator.service.getClasses().subscribe((res) => {
            expect(res).toEqual([]);
        });

        const req = spectator.expectOne(STYLE_CLASSES_FILE_URL, HttpMethod.GET);

        req.flush({ bad: 'format' });
    });
});
