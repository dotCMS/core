import { TestBed } from '@angular/core/testing';

import { EmaAppConfigurationService } from './ema-app-configuration.service';

describe('EmaAppConfigurationService', () => {
    let service: EmaAppConfigurationService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(EmaAppConfigurationService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
