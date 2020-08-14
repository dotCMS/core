import { TestBed } from '@angular/core/testing';
import { DotWizardService } from './dot-wizard.service';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';

describe('DotWizardService', () => {
    let service: DotWizardService;
    const mockData = { id: '11', name: 'DotCMS' };
    const mockWizardSteps: DotWizardStep<any>[] = [{ component: 'test', data: { id: '12' } }];

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotWizardService] });
        service = TestBed.get(DotWizardService);
    });

    it('should receive the steps', () => {
        let steps: DotWizardStep<any>[];
        service.showDialog$.subscribe(data => {
            steps = data;
        });
        service.open(mockWizardSteps);
        expect(steps).toEqual(mockWizardSteps);
    });

    it('should receive output on open subscription', () => {
        let outputData = null;
        service.open(mockWizardSteps).subscribe(data => {
            outputData = data;
        });
        service.output$(mockData);
        expect(outputData).toEqual(mockData);
    });
});
