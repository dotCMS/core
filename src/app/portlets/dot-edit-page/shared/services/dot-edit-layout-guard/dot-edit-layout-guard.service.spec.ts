import { DotEditLayoutGuardService } from './dot-edit-layout-guard.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { ActivatedRouteSnapshot } from '@angular/router';

const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', ['toString']);
route.parent = {
    data: {}
};

describe('DotEditLayoutGuardService', () => {
    let guard: DotEditLayoutGuardService;

    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotEditLayoutGuardService,
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: route
                },
            ],
            imports: []
        });

        guard = testbed.get(DotEditLayoutGuardService);
    });

    it('should return true by default', () => {
        expect(guard.canActivate(route, null)).toBe(true);
    });

    it('should return true when user can edit the page', () => {
        route.parent.data = {
            content: {
                page: {
                    canEdit: true
                }
            }
        };

        expect(guard.canActivate(route, null)).toBe(true);
    });

    it('should return false when user can not edit the page', () => {
        route.parent.data = {
            content: {
                page: {
                    canEdit: false
                }
            }
        };

        expect(guard.canActivate(route, null)).toBe(false);
    });
});
