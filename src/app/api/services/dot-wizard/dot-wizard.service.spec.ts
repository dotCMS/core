import { TestBed } from '@angular/core/testing';
import { DotWizardService } from './dot-wizard.service';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';

describe('DotWizardService', () => {
    let service: DotWizardService;
    const mockOutput = { id: '11', name: 'DotCMS' };
    const mockWizardSteps: DotWizardStep<any>[] = [{ component: 'test', data: { id: '12' } }];
    const mockWizardInput: DotWizardInput = {
        steps: mockWizardSteps,
        title: 'Wizard'
    };
    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotWizardService] });
        service = TestBed.get(DotWizardService);
    });

    it('should receive the steps', () => {
        let data: DotWizardInput;
        service.showDialog$.subscribe(result => {
            data = result;
        });
        service.open(mockWizardInput);
        expect(data).toEqual(mockWizardInput);
    });

    it('should receive output on open subscription', () => {
        let outputData = null;
        service.open(mockWizardInput).subscribe(data => {
            outputData = data;
        });
        service.output$(mockOutput);
        expect(outputData).toEqual(mockOutput);
    });
});
