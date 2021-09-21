import { TestBed } from '@angular/core/testing';
import { DotGenerateSecurePasswordService } from './dot-generate-secure-password.service';

describe('DotGenerateSecurePasswordService', () => {
    let service: DotGenerateSecurePasswordService;
    let password: string;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotGenerateSecurePasswordService] });
        service = TestBed.inject(DotGenerateSecurePasswordService);
        service.showDialog$.subscribe((data) => (password = data.password));
    });

    it('should receive bundleID', () => {
        service.open({ password: 'ZXC!2' });
        expect(password).toEqual('ZXC!2');
    });
});
