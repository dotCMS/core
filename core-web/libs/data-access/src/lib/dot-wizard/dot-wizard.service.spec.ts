import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotWizardInput, DotWizardStep } from '@dotcms/dotcms-models';

import { DotWizardService } from './dot-wizard.service';

describe('DotWizardService', () => {
    let spectator: SpectatorService<DotWizardService>;
    let service: DotWizardService;
    let data: DotWizardInput;
    let outputData: { [key: string]: string };

    const mockOutput = { id: '11', name: 'DotCMS' };
    const mockWizardSteps: DotWizardStep[] = [{ component: 'test', data: { id: '12' } }];
    const mockWizardInput: DotWizardInput = {
        steps: mockWizardSteps,
        title: 'Wizard'
    };

    const createService = createServiceFactory(DotWizardService);

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    it('should receive the steps', () => {
        service.showDialog$.subscribe((result) => {
            data = result;
        });
        service.open(mockWizardInput);
        expect(data).toEqual(mockWizardInput);
    });

    it('should receive output on open subscription', () => {
        service.open(mockWizardInput).subscribe((data) => {
            outputData = data;
        });
        service.output$(mockOutput);
        expect(outputData).toEqual(mockOutput);
    });
});
