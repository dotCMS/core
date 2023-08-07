import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRouteSnapshot } from '@angular/router';

import {
    DotExperimentExperimentResolver,
    DotExperimentsService
} from '@dotcms/portlets/dot-experiments/data-access';
import { getExperimentMock } from '@dotcms/utils-testing';

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentExperimentResolver', () => {
    let spectator: SpectatorService<DotExperimentExperimentResolver>;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let activatedRouteSnapshotMock: SpyObject<ActivatedRouteSnapshot>;

    const createService = createServiceFactory({
        service: DotExperimentExperimentResolver,
        providers: [mockProvider(ActivatedRouteSnapshot), mockProvider(DotExperimentsService)]
    });
    beforeEach(() => {
        spectator = createService();
        dotExperimentsService = spectator.inject(DotExperimentsService);
        activatedRouteSnapshotMock = spectator.inject(ActivatedRouteSnapshot);
    });

    it("shouldn't get a experiment by experimentId", () => {
        activatedRouteSnapshotMock.queryParams = {};

        spectator.service.resolve(activatedRouteSnapshotMock).subscribe((result) => {
            expect(result).toBe(null);
        });
    });

    it('should get a experiment by experimentId', () => {
        const experimentId = '123';
        activatedRouteSnapshotMock.queryParams = { experimentId };

        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        spectator.service.resolve(activatedRouteSnapshotMock).subscribe((result) => {
            expect(result).toBe(EXPERIMENT_MOCK);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(experimentId);
        });
    });
});
