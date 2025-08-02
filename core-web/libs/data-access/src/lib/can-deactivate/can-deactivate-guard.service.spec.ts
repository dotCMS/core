import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { CanDeactivateGuardService } from './can-deactivate-guard.service';

import { DotRouterService } from '../dot-router/dot-router.service';

describe('CanDeactivateGuardService', () => {
    let service: CanDeactivateGuardService;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [CanDeactivateGuardService, DotRouterService]
        });
        service = TestBed.inject(CanDeactivateGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should let the user leave the route when allowRouteDeactivation has been called', (done) => {
        dotRouterService.allowRouteDeactivation();
        service.canDeactivate().subscribe((deactivate) => {
            expect(deactivate).toBeTruthy();
            done();
        });
    });

    it('canBeDesactivated should be false', () => {
        dotRouterService.forbidRouteDeactivation();
        service.canDeactivate().subscribe(() => {
            fail('Should not be called if canBeDesactivated is false');
        });
    });

    it('should set request a page leave', (done) => {
        dotRouterService.pageLeaveRequest$.subscribe(() => {
            done();
        });
        dotRouterService.forbidRouteDeactivation();
        service.canDeactivate().subscribe(() => {
            fail('Should not be called if canBeDesactivated is false');
        });
    });
});
