import { TestBed } from '@angular/core/testing';

import { CanDeactivateGuardService } from './can-deactivate-guard.service';

import { DotEditLayoutService } from '../dot-edit-layout/dot-edit-layout.service';

describe('LayoutEditorCanDeactivateGuardService', () => {
    let service: CanDeactivateGuardService;
    let dotEditLayoutService: DotEditLayoutService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [CanDeactivateGuardService, DotEditLayoutService]
        });
        service = TestBed.inject(CanDeactivateGuardService);
        dotEditLayoutService = TestBed.inject(DotEditLayoutService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should le the user left the route when _canBeDesactivated is true', (done) => {
        dotEditLayoutService.changeDesactivateState(true);
        service.canDeactivate().subscribe((deactivate) => {
            expect(deactivate).toBeTruthy();
            done();
        });
    });

    it('canBeDesactivated should be false', () => {
        dotEditLayoutService.changeDesactivateState(false);
        service.canDeactivate().subscribe(() => {
            fail('Should not be called if canBeDesactivated is false');
        });
    });

    it('should set _closeEditLayout when canBeDesactivated is false', (done) => {
        dotEditLayoutService.closeEditLayout$.subscribe((resp) => {
            expect(resp).toBeTruthy();
            done();
        });
        dotEditLayoutService.changeDesactivateState(false);
        service.canDeactivate().subscribe(() => {
            fail('Should not be called if canBeDesactivated is false');
        });
    });
});
