import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotAgentRunService } from '@dotcms/data-access';

import { DotA11yAgentService } from './dot-a11y-agent.service';

import { AgentFixRequest } from '../models/accessibility-studio.models';

const REQUEST: AgentFixRequest = {
    identifier: 'id-1',
    languageId: 1,
    skipCss: false
};

describe('DotA11yAgentService', () => {
    let spectator: SpectatorService<DotA11yAgentService>;
    let service: DotA11yAgentService;
    let runService: jest.Mocked<DotAgentRunService>;

    const createService = createServiceFactory({
        service: DotA11yAgentService,
        providers: [
            mockProvider(DotAgentRunService, {
                run: jest.fn().mockReturnValue(of()),
                stop: jest.fn().mockReturnValue(of())
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        runService = spectator.inject(DotAgentRunService) as jest.Mocked<DotAgentRunService>;
    });

    it('fixStream delegates to the generic run service with the a11y stream endpoint', () => {
        service.fixStream(REQUEST).subscribe();
        expect(runService.run).toHaveBeenCalledWith('/api/v1/agent/a11y/fix/stream', REQUEST);
    });

    it('stop delegates to the generic run service with the a11y stop endpoint', () => {
        service.stop().subscribe();
        expect(runService.stop).toHaveBeenCalledWith('/api/v1/agent/a11y/stop');
    });
});
