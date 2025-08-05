import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { ActivatedRouteSnapshot } from '@angular/router';

import { DotExperimentsConfigResolver } from './dot-experiments-config-resolver';

describe('DotExperimentsConfigResolver', () => {
    let spectator: SpectatorHttp<DotExperimentsConfigResolver>;
    const createHttp = createHttpFactory(DotExperimentsConfigResolver);

    beforeEach(() => (spectator = createHttp()));

    it('should get configuration keys from the server', () => {
        const route = {
            data: { experimentsConfigProps: ['test', 'test2'] }
        } as unknown as ActivatedRouteSnapshot;

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        spectator.service.resolve(route).subscribe();
        spectator.expectOne(`/api/v1/configuration/config?keys=test,test2`, HttpMethod.GET);
    });
});
