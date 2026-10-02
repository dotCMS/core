import { isObservable, Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotCurrentUserService } from '@dotcms/data-access';

import { dotContentTypeTabsResolver } from './dot-content-type-tabs.resolver';

const mockRoute = {} as ActivatedRouteSnapshot;
const mockState = {} as RouterStateSnapshot;

/**
 * `ResolveFn` declares `MaybeAsync<T>`, so the annotation on the resolver hides the fact that it
 * always returns an observable and `.subscribe()` does not type-check. `isObservable` is rxjs's own
 * type guard, so this narrows without a cast.
 */
function asObservable<T>(result: T | Observable<T> | Promise<T>): Observable<T> {
    if (!isObservable(result)) {
        throw new Error('Expected the resolver to return an Observable');
    }

    return result;
}

const runResolver = (): Observable<DotContentTypeTabsResolvedData> =>
    asObservable(
        TestBed.runInInjectionContext(() => dotContentTypeTabsResolver(mockRoute, mockState))
    );

describe('dotContentTypeTabsResolver', () => {
    let dotCurrentUserService: DotCurrentUserService;

    const setup = (hasAccess: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                HttpClient,
                {
                    provide: DotCurrentUserService,
                    useValue: { hasAccessToPortlet: vi.fn().mockReturnValue(of(hasAccess)) }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotCurrentUserService = TestBed.inject(DotCurrentUserService);
    };

    it('should resolve showPermissionsTab as true when user has access', () =>
        new Promise<void>((done) => {
            setup(true);

            runResolver().subscribe((result) => {
                expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith(
                    'permissions'
                );
                expect(result).toEqual({ showPermissionsTab: true });
                done();
            });
        }));

    it('should resolve showPermissionsTab as false when user lacks access', () =>
        new Promise<void>((done) => {
            setup(false);

            runResolver().subscribe((result) => {
                expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith(
                    'permissions'
                );
                expect(result).toEqual({ showPermissionsTab: false });
                done();
            });
        }));
});
