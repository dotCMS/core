import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotVersionableService, DotVersionable } from './dot-versionable.service';

describe('DotVersionableService', () => {
    let spectator: SpectatorHttp<DotVersionableService>;
    let service: DotVersionableService;

    const mockVersionableEntity = { inode: '123' };
    const mockDeleteEntity = { success: true };

    const createHttp = createHttpFactory(DotVersionableService);

    beforeEach(() => {
        spectator = createHttp();
        service = spectator.service;
    });

    it('should bring back version successfully', (done) => {
        const mockResponse = { entity: mockVersionableEntity };

        service.bringBack('123').subscribe((res: DotVersionable) => {
            expect(res).toEqual(mockVersionableEntity);
            done();
        });

        const req = spectator.expectOne('/api/v1/versionables/123/_bringback', HttpMethod.PUT);
        req.flush(mockResponse);
    });

    it('should delete version successfully', (done) => {
        const mockResponse = { entity: mockDeleteEntity };

        service.deleteVersion('123').subscribe((res) => {
            expect(res).toEqual(mockDeleteEntity);
            done();
        });

        const req = spectator.expectOne('/api/v1/versionables/123', HttpMethod.DELETE);
        req.flush(mockResponse);
    });
});
