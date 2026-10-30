/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotContentletLockerService,
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotLicenseService,
    DotMessageDisplayService,
    DotPageRenderService,
    DotPageStateService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';
import {
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';

import { DotIframePortletLegacyResolver } from './dot-iframe-porlet-legacy-resolver.service';

import { DOTTestBed } from '../../../../../test/dot-test-bed';

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

    beforeEach(waitForAsync(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotSessionStorageService,
                DotPageStateService,
                DotIframePortletLegacyResolver,
                DotPageRenderService,
                DotExperimentsService,
                DotContentletLockerService,
                DotLicenseService,
                DotESContentService,
                DotFavoritePageService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
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
        const mock = new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));
        dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));
        spyOn(dotLicenseService, 'canAccessEnterprisePortlet').and.returnValue(of(true));

        resolver.resolve(route, state).subscribe((canAccess: boolean) => {
            expect(canAccess).toEqual(true);
        });
    });
});
