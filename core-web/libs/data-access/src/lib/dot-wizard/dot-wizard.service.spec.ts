/* eslint-disable @typescript-eslint/no-explicit-any */

import { TestBed } from '@angular/core/testing';

import { DotWizardInput, DotWizardStep } from '@dotcms/dotcms-models';

import { DotWizardService } from './dot-wizard.service';

describe('DotWizardService', () => {
    let service: DotWizardService;
    let data: DotWizardInput;
    let outputData: { [key: string]: string };

    const mockOutput = { id: '11', name: 'DotCMS' };
    const mockWizardSteps: DotWizardStep[] = [{ component: 'test', data: { id: '12' } }];
    const mockWizardInput: DotWizardInput = {
        steps: mockWizardSteps,
        title: 'Wizard'
    };
    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotWizardService] });
        service = TestBed.get(DotWizardService);
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
