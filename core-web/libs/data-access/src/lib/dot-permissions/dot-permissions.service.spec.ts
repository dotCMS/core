import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { ASSET_PERMISSIONS_URL, DotPermissionsService } from './dot-permissions.service';

describe('DotPermissionsService', () => {
    let spectator: SpectatorHttp<DotPermissionsService>;

    const createHttp = createHttpFactory(DotPermissionsService);

    beforeEach(() => {
        spectator = createHttp();
    });

    // What the calling user may do to a folder opened from a Content Drive link, so its dialog
    // follows the same rules as the context menu that would have opened it (#37759, FR-033).
    describe('getUserAccess', () => {
        it('should read the edit flags from the asset permissions endpoint', () =>
            new Promise<void>((done) => {
                spectator.service.getUserAccess('folder-1').subscribe((access) => {
                    expect(access).toEqual({ canEdit: true, canEditPermissions: false });
                    done();
                });

                spectator
                    .expectOne(`${ASSET_PERMISSIONS_URL}/folder-1`, HttpMethod.GET)
                    .flush({ entity: { canEdit: true, canEditPermissions: false } });
            }));

        it('should read an absent flag as not allowed', () =>
            new Promise<void>((done) => {
                spectator.service.getUserAccess('folder-1').subscribe((access) => {
                    expect(access).toEqual({ canEdit: false, canEditPermissions: false });
                    done();
                });

                spectator
                    .expectOne(`${ASSET_PERMISSIONS_URL}/folder-1`, HttpMethod.GET)
                    .flush({ entity: {} });
            }));
    });

    describe('canAddChildren', () => {
        it('should request the asset permissions endpoint for the given asset', () => {
            spectator.service.canAddChildren('site-123').subscribe();

            const req = spectator.expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET);

            expect(req.request.method).toBe('GET');
        });

        it('should emit true when the user can add children', () =>
            new Promise<void>((done) => {
                spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                    expect(canAdd).toBe(true);
                    done();
                });

                spectator
                    .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                    .flush({ entity: { canAddChildren: true } });
            }));

        it('should emit false when the user cannot add children', () =>
            new Promise<void>((done) => {
                spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                    expect(canAdd).toBe(false);
                    done();
                });

                spectator
                    .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                    .flush({ entity: { canAddChildren: false } });
            }));

        // An older instance answers without the field. Treating `undefined` as "denied" would strip
        // the creation buttons from every user on that instance, so the optimistic read is the safe
        // one: the server still refuses the write.
        it('should emit true when the response omits canAddChildren', () =>
            new Promise<void>((done) => {
                spectator.service.canAddChildren('site-123').subscribe((canAdd) => {
                    expect(canAdd).toBe(true);
                    done();
                });

                spectator
                    .expectOne(`${ASSET_PERMISSIONS_URL}/site-123`, HttpMethod.GET)
                    .flush({ entity: {} });
            }));
    });
});
