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

    it('should complete the stream without emitting when cancel is called', () => {
        const next = jest.fn();
        const complete = jest.fn();
        service.open(mockWizardInput).subscribe({ next, complete });
        service.cancel();
        expect(next).not.toHaveBeenCalled();
        expect(complete).toHaveBeenCalledTimes(1);
    });

    it('should not deliver output from a new open() to a previous (cancelled) subscription', () => {
        const firstNext = jest.fn();
        const secondNext = jest.fn();

        service.open(mockWizardInput).subscribe(firstNext);
        service.cancel(); // user dismissed the first wizard

        service.open(mockWizardInput).subscribe(secondNext);
        service.output$(mockOutput); // user sends on the second wizard

        expect(firstNext).not.toHaveBeenCalled();
        expect(secondNext).toHaveBeenCalledWith(mockOutput);
    });

    it('should complete a previous stream when open() is called again without cancel', () => {
        const firstNext = jest.fn();
        const firstComplete = jest.fn();
        const secondNext = jest.fn();

        service.open(mockWizardInput).subscribe({ next: firstNext, complete: firstComplete });
        service.open(mockWizardInput).subscribe(secondNext);
        service.output$(mockOutput);

        expect(firstNext).not.toHaveBeenCalled();
        expect(firstComplete).toHaveBeenCalledTimes(1);
        expect(secondNext).toHaveBeenCalledWith(mockOutput);
    });

    it('should complete after output$ so take(1) consumers unsubscribe cleanly', () => {
        const next = jest.fn();
        const complete = jest.fn();
        service.open(mockWizardInput).subscribe({ next, complete });
        service.output$(mockOutput);
        expect(next).toHaveBeenCalledWith(mockOutput);
        expect(complete).toHaveBeenCalledTimes(1);
    });
});
