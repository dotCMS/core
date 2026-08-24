import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { ASSET_PERMISSIONS_URL, DotPermissionsService } from './dot-permissions.service';

describe('DotPermissionsService', () => {
    let spectator: SpectatorHttp<DotPermissionsService>;

    const createHttp = createHttpFactory(DotPermissionsService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('canAddChildren', () => {
        it('should request the asset permissions endpoint for the given asset', () => {
            spectator.service.canAddChildren('site-123').subscribe();

            const req = spectator.expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET);

            expect(req.request.method).toBe('GET');
        });

        it('should emit true when the user can add children', (done) => {
            spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                expect(canAdd).toBe(true);
                done();
            });

            spectator
                .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                .flush({ entity: { canAddChildren: true } });
        });

        it('should emit false when the user cannot add children', (done) => {
            spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                expect(canAdd).toBe(false);
                done();
            });

            spectator
                .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                .flush({ entity: { canAddChildren: false } });
        });

        // An older instance answers without the field. Treating `undefined` as "denied" would strip
        // the creation buttons from every user on that instance, so the optimistic read is the safe
        // one: the server still refuses the write.
        it('should emit true when the response omits canAddChildren', (done) => {
            spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                expect(canAdd).toBe(true);
                done();
            });

            spectator
                .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                .flush({ entity: {} });
        });
    });
});
