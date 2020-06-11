import { LoginService } from 'dotcms-js';
import { LoginServiceMock, mockUser } from './../../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { mockDotRenderedPage } from '../../../../../test/dot-page-render.mock';
import { of } from 'rxjs';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { DotPageStateService } from '@portlets/dot-edit-page/content/services/dot-page-state/dot-page-state.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageRenderState, DotPageRender } from '@portlets/dot-edit-page/shared/models';
import { DotIframePortletLegacyResolver } from './dot-iframe-porlet-legacy-resolver.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);

const state: any = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);

route.queryParams = {};

describe('DotIframePorletLegacyResolver', () => {
    let dotPageStateService: DotPageStateService;
    let dotPageStateServiceRequestPageSpy: jasmine.Spy;
    let resolver: DotIframePortletLegacyResolver;
    let dotLicenseService: DotLicenseService;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotPageStateService,
                DotIframePortletLegacyResolver,
                DotPageRenderService,
                DotContentletLockerService,
                DotLicenseService,
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: route
                },
                {
                    provide: RouterStateSnapshot,
                    useValue: state
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            imports: [RouterTestingModule]
        });

        dotPageStateService = testbed.get(DotPageStateService);
        dotPageStateServiceRequestPageSpy = spyOn(dotPageStateService, 'requestPage');
        resolver = testbed.get(DotIframePortletLegacyResolver);
        dotLicenseService = testbed.get(DotLicenseService);
        state.url = '/rules';
    }));

    it('should return if user can access url to be rendered with current license', () => {
        const mock = new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage));
        dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));
        spyOn(dotLicenseService, 'canAccessEnterprisePortlet').and.returnValue(of(true));

        resolver.resolve(route, state).subscribe((canAccess: boolean) => {
            expect(canAccess).toEqual(true);
        });
    });
});
