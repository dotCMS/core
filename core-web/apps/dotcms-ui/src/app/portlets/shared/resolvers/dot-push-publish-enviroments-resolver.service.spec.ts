import { SpectatorService, SpyObject } from '@ngneat/spectator';
import { createServiceFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotPushPublishEnvironmentsResolver } from '@portlets/shared/resolvers/dot-push-publish-enviroments-resolver.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';

describe('DotPushPublishEnvironmentsResolver', () => {
    let spectator: SpectatorService<DotPushPublishEnvironmentsResolver>;
    let pushPublishService: SpyObject<PushPublishService>;

    const createService = createServiceFactory({
        service: DotPushPublishEnvironmentsResolver,
        providers: [mockProvider(PushPublishService)]
    });

    beforeEach(() => {
        spectator = createService();
        pushPublishService = spectator.inject(PushPublishService);
    });

    it('should get a list of environments', () => {
        spectator.service.resolve().subscribe();
        expect(pushPublishService.getEnvironments).toHaveBeenCalled();
    });
});
