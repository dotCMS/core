import { TestBed } from '@angular/core/testing';

import { EmaAppSecretsService } from './ema-app-secrets.service';

describe('EmaAppSecretsService', () => {
    let service: EmaAppSecretsService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(EmaAppSecretsService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
