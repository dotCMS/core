import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCurrentUserService } from '@dotcms/data-access';

import {
    DotContentTypeTabsResolver,
    DotContentTypeTabsResolvedData
} from './dot-content-type-tabs.resolver';

describe('DotContentTypeTabsResolver', () => {
    let resolver: DotContentTypeTabsResolver;
    let dotCurrentUserService: DotCurrentUserService;

    const setup = (hasAccess: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                DotContentTypeTabsResolver,
                HttpClient,
                {
                    provide: DotCurrentUserService,
                    useValue: { hasAccessToPortlet: jest.fn().mockReturnValue(of(hasAccess)) }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        resolver = TestBed.inject(DotContentTypeTabsResolver);
        dotCurrentUserService = TestBed.inject(DotCurrentUserService);
    };

    it('should resolve showPermissionsTab as true when user has access', (done) => {
        setup(true);

        resolver.resolve().subscribe((result: DotContentTypeTabsResolvedData) => {
            expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith('permissions');
            expect(result).toEqual({ showPermissionsTab: true });
            done();
        });
    });

    it('should resolve showPermissionsTab as false when user lacks access', (done) => {
        setup(false);

        resolver.resolve().subscribe((result: DotContentTypeTabsResolvedData) => {
            expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith('permissions');
            expect(result).toEqual({ showPermissionsTab: false });
            done();
        });
    });
});
