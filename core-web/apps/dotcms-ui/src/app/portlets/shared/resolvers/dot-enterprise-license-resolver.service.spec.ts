import { SpectatorService, SpyObject } from '@ngneat/spectator';
import { createServiceFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotLicenseService } from '@dotcms/data-access';
import { DotEnterpriseLicenseResolver } from '@portlets/shared/resolvers/dot-enterprise-license-resolver.service';

describe('DotEnterpriseLicenseResolver', () => {
    let spectator: SpectatorService<DotEnterpriseLicenseResolver>;
    let dotLicenseService: SpyObject<DotLicenseService>;

    const createService = createServiceFactory({
        service: DotEnterpriseLicenseResolver,
        providers: [mockProvider(DotLicenseService)]
    });

    beforeEach(() => {
        spectator = createService();
        dotLicenseService = spectator.inject(DotLicenseService);
    });

    it('should get a list of environments', () => {
        spectator.service.resolve().subscribe();
        expect(dotLicenseService.isEnterprise).toHaveBeenCalled();
    });
});
