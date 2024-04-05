import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotEmaWorkflowActionsService } from './dot-ema-workflow-actions.service';

describe('DotEmaWorkflowActionsService', () => {
    let spectator: SpectatorService<DotEmaWorkflowActionsService>;

    const createService = createServiceFactory(DotEmaWorkflowActionsService);

    beforeEach(() => {
        spectator = createService();
    });

    it('should fail because i dont have any test at the moment', () => {
        expect(spectator).toBeFalsy();
    });
});
