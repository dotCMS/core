import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotCurrentUserService } from '@dotcms/data-access';

import {
    DotContentTypeTabsResolvedData,
    dotContentTypeTabsResolver
} from './dot-content-type-tabs.resolver';

const mockRoute = {} as ActivatedRouteSnapshot;
const mockState = {} as RouterStateSnapshot;

describe('dotContentTypeTabsResolver', () => {
    let dotCurrentUserService: DotCurrentUserService;

    const setup = (hasAccess: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                HttpClient,
                {
                    provide: DotCurrentUserService,
                    useValue: { hasAccessToPortlet: jest.fn().mockReturnValue(of(hasAccess)) }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotCurrentUserService = TestBed.inject(DotCurrentUserService);
    };

    it('should resolve showPermissionsTab as true when user has access', (done) => {
        setup(true);

        TestBed.runInInjectionContext(() =>
            dotContentTypeTabsResolver(mockRoute, mockState)
        ).subscribe((result: DotContentTypeTabsResolvedData) => {
            expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith('permissions');
            expect(result).toEqual({ showPermissionsTab: true });
            done();
        });
    });

    it('should resolve showPermissionsTab as false when user lacks access', (done) => {
        setup(false);

        TestBed.runInInjectionContext(() =>
            dotContentTypeTabsResolver(mockRoute, mockState)
        ).subscribe((result: DotContentTypeTabsResolvedData) => {
            expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith('permissions');
            expect(result).toEqual({ showPermissionsTab: false });
            done();
        });
    });
});
