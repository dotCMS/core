import { TestBed } from '@angular/core/testing';

import { DotNavLogoService } from './dot-nav-logo.service';

describe('DotNavLogoService', () => {
    let service: DotNavLogoService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: []
        });
        service = TestBed.inject(DotNavLogoService);
    });

    it('should not set a new logo', (done) => {
        service.setLogo(null);
        service.navBarLogo$.subscribe((logo) => {
            expect(logo).toBeNull();
            done();
        });
    });

    it('should set a new logo', (done) => {
        service.setLogo('/dA/id/asset/logo.png');
        service.navBarLogo$.subscribe((logo) => {
            expect(logo).toBe('url("/dA/id/asset/logo.png")');
            done();
        });
    });

    it("should not set a logo if the logo string doesn't starts with /dA", (done) => {
        service.setLogo('FL');
        service.navBarLogo$.subscribe((logo) => {
            expect(logo).toBeNull();
            done();
        });
    });
});
