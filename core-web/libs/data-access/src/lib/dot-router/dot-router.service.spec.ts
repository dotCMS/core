/* eslint-disable @typescript-eslint/no-explicit-any */
import { expect, describe } from '@jest/globals';
import { Subject } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginService } from '@dotcms/dotcms-js';

import { DotRouterService } from './dot-router.service';

class RouterMock {
    _events: Subject<any> = new Subject();
    url = '/c/test';

    routerState = {
        snapshot: {
            url: '/c/hello-world'
        }
    };

    navigate = jest.fn(() => {
        return new Promise((resolve) => {
            resolve(true);
        });
    });

    navigateByUrl = jest.fn(() => {
        return new Promise((resolve) => {
            resolve(true);
        });
    });

    createUrlTree = jest.fn((link) => {
        return link;
    });

    get events() {
        return this._events.asObservable();
    }

    getCurrentNavigation(): { finalUrl: { queryParams: { [key: string]: string } } } | null {
        return {
            finalUrl: {
                queryParams: {}
            }
        };
    }

    triggerNavigationEnd(url: string): void {
        this._events.next(new NavigationEnd(0, url || '/url/678', url || '/url/789'));
    }
}

class ActivatedRouteMock {
    snapshot = {
        queryParams: {
            hello: 'world'
        }
    };
}

describe('DotRouterService', () => {
    let service: DotRouterService;
    let router: InstanceType<typeof RouterMock>;

    beforeEach(waitForAsync(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotRouterService,
                {
                    provide: LoginService,
                    useValue: {}
                },
                {
                    provide: Router,
                    useClass: RouterMock
                },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                }
            ],
            imports: [RouterTestingModule]
        });

        service = testbed.inject(DotRouterService);
        router = testbed.inject(Router) as unknown as InstanceType<typeof RouterMock>;
    }));

    it('should set current url value', () => {
        expect(service.currentSavedURL).toEqual(router.url);
    });

    it('should set previous & current url value', () => {
        router.triggerNavigationEnd('/newUrl');
        expect(service.routeHistory).toEqual({ url: '/newUrl', previousUrl: '/c/test' });
        expect(service.currentSavedURL).toEqual('/newUrl');
    });

    it('should get queryParams from Router', () => {
        jest.spyOn(router, 'getCurrentNavigation').mockReturnValue({
            finalUrl: {
                queryParams: {
                    hola: 'mundo'
                }
            }
        });

        expect(service.queryParams).toEqual({
            hola: 'mundo'
        });
    });

    it('should get queryParams from ActivatedRoute', () => {
        jest.spyOn(router, 'getCurrentNavigation').mockReturnValue(null);
        expect(service.queryParams).toEqual({
            hello: 'world'
        });
    });

    it('should go to main', () => {
        service.goToMain();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should go to edit page', () => {
        jest.spyOn(service, 'goToEditPage');
        service.goToMain('/about/us');
        expect(service.goToEditPage).toHaveBeenCalledWith({ url: '/about/us' });
    });

    it('should go to Starter page', () => {
        service.goToStarter();
        expect(router.navigate).toHaveBeenCalledWith(['/starter']);
    });

    it('should go to Content page', () => {
        service.goToContent();
        expect(router.navigate).toHaveBeenCalledWith(['/c/content']);
    });

    it('should go to edit page', () => {
        service.goToEditTemplate('123');
        expect(router.navigate).toHaveBeenCalledWith(['/templates/edit/123']);
    });

    it('should go to edit page with inode', () => {
        service.goToEditTemplate('123', '456');

        expect(router.navigate).toHaveBeenCalledWith(['/templates/edit/123/inode/456']);
    });

    it('should go to edit content type page', () => {
        service.goToEditContentType('123', 'Form');
        expect(router.navigate).toHaveBeenCalledWith(['/Form/edit/123']);
    });

    it('should go to storedRedirectUrl', () => {
        service.storedRedirectUrl = 'test/fake';
        service.goToMain();
        expect(router.navigate).toHaveBeenCalledWith(['test/fake']);
    });

    it('should go to previous URL when goToMain() called', () => {
        service.routeHistory = { previousUrl: 'test/fake', url: '/' };
        service.goToPreviousUrl();
        expect(router.navigate).toHaveBeenCalledWith(['test/fake']);
    });

    it('should go to edit page', () => {
        service.goToEditPage({ url: 'abc/def' });
        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: { url: 'abc/def' },
            state: {
                menuId: 'edit-page'
            }
        });
    });

    it('should go to edit page with language_id', () => {
        service.goToEditPage({ url: 'abc/def', language_id: '1' });
        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: { url: 'abc/def', language_id: '1' },
            state: {
                menuId: 'edit-page'
            }
        });
    });

    it('should go to edit contentlet', () => {
        service.goToEditContentlet('123');
        expect(router.navigate).toHaveBeenCalledWith(['/c/hello-world/123'], {
            queryParamsHandling: 'preserve'
        });
    });

    it('should go to edit workflow task', () => {
        service.goToEditTask('123');
        expect(router.navigate).toHaveBeenCalledWith(['/c/workflow/123']);
    });

    it('should go to create content route with provided content type variable name', () => {
        service.goToCreateContent('persona');
        expect(router.navigate).toHaveBeenCalledWith(['/c/content/new/persona']);
    });

    it('should go to create integration service', () => {
        service.goToUpdateAppsConfiguration('123', { configured: false, name: 'test', id: '1' });
        expect(router.navigate).toHaveBeenCalledWith(['/apps/123/create/1']);
    });

    it('should go to edit integration service', () => {
        service.goToUpdateAppsConfiguration('123', { configured: true, name: 'test', id: '1' });
        expect(router.navigate).toHaveBeenCalledWith(['/apps/123/edit/1']);
    });

    it('should return true if a portlet is jsp', () => {
        expect(service.isJSPPortlet()).toBeTruthy();
    });

    it('should return true if the URL is pointing to a portlet is jsp', () => {
        expect(service.isJSPPortletURL('/c/test')).toBeTruthy();
    });

    it('should return false if the URL is not pointing to a portlet is jsp', () => {
        expect(service.isJSPPortletURL('/test')).toBeFalsy();
    });

    it('should return true if edit page url', () => {
        router.routerState.snapshot.url = 'edit-page';
        expect(service.isEditPage()).toBe(true);
    });

    it('should return true if the portletid is a custom portlet', () => {
        expect(service.isCustomPortlet('c_testing')).toBe(true);
    });

    it('should return false if the portletid is not a custom portlet', () => {
        expect(service.isCustomPortlet('site-browser')).toBe(false);
    });

    it('should return false if the currentPortlet is not a custom portlet', () => {
        expect(service.isCustomPortlet('site-browser')).toBe(false);
    });

    it('should return true if the currentPortlet is not a custom portlet', () => {
        router.routerState.snapshot.url = '/c/c-testing';
        expect(service.isCustomPortlet('site-browser')).toBe(false);
    });

    it('should got to porlet by URL', () => {
        service.gotoPortlet('/c/test');
        expect(router.createUrlTree).toHaveBeenCalledWith(['/c/test'], {
            queryParamsHandling: '',
            queryParams: {}
        });
        expect(router.navigateByUrl).toHaveBeenCalledWith(['/c/test'], {
            replaceUrl: false,
            state: { menuId: undefined }
        });
    });

    it('should go to porlet by URL and keep the queryParams', () => {
        service.gotoPortlet('/c/test?filter="Blog"', {
            queryParamsHandling: 'preserve',
            replaceUrl: false
        });
        expect(router.createUrlTree).toHaveBeenCalledWith(['/c/test?filter="Blog"'], {
            queryParamsHandling: 'preserve',
            queryParams: {}
        });
        expect(router.navigateByUrl).toHaveBeenCalledWith(['/c/test?filter="Blog"'], {
            replaceUrl: false,
            state: { menuId: undefined }
        });
    });

    it('should go to porlet by URL with queryParams', () => {
        const queryParams = {
            folderId: '123',
            path: '/images'
        };

        service.gotoPortlet('/c/content-drive', {
            queryParams
        });

        expect(router.createUrlTree).toHaveBeenCalledWith(['/c/content-drive'], {
            queryParamsHandling: '',
            queryParams: {
                folderId: '123',
                path: '/images'
            }
        });
        expect(router.navigateByUrl).toHaveBeenCalledWith(['/c/content-drive'], {
            replaceUrl: false,
            state: { menuId: undefined }
        });
    });

    it('should return the correct  Portlet Id', () => {
        expect(service.getPortletId('#/c/content?test=value')).toBe('content');
        expect(service.getPortletId('/c/add/content?fds=ds')).toBe('content');
        expect(
            service.getPortletId(
                'c/content%3Ffilter%3DProducts/19d3aecc-5b68-4d98-ba1b-297d5859403c'
            )
        ).toBe('content');
    });

    it('should navigate replacing URL params', () => {
        const params = { id: 'content' };
        service.replaceQueryParams(params);
        expect(router.navigate).toHaveBeenCalledWith([], {
            queryParams: params,
            queryParamsHandling: 'merge'
        });
    });

    it('Should set canDeactivateRoute$ to true', (done) => {
        service.allowRouteDeactivation();
        service.canDeactivateRoute$.subscribe((resp) => {
            expect(resp).toBe(true);
            done();
        });
    });

    it('Should set canDeactivateRoute$ to false', (done) => {
        service.forbidRouteDeactivation();
        service.canDeactivateRoute$.subscribe((resp) => {
            expect(resp).toBe(false);
            done();
        });
    });

    it('trigger pageLeaveRequest observable stream when a page leave is requested', (done) => {
        const leavePage = jest.fn();
        service.pageLeaveRequest$.subscribe(() => {
            leavePage();
            expect(leavePage).toHaveBeenCalled();
            done();
        });
        service.requestPageLeave();
    });

    describe('go to login', () => {
        beforeEach(() => {
            const mockDate = new Date(1466424490000);
            jest.useFakeTimers().setSystemTime(mockDate);
        });

        it('should add the cache busting', () => {
            service.goToLogin();
            expect(router.navigate).toHaveBeenCalledWith(['/public/login'], {
                queryParams: { r: 1466424490000 }
            });
            jest.useRealTimers(); // We need to deactivate the fake timer
        });

        it('should go to login with cache busting', () => {
            service.goToLogin({
                queryParams: { test: 'test' }
            });
            expect(router.navigate).toHaveBeenCalledWith(['/public/login'], {
                queryParams: { test: 'test', r: 1466424490000 }
            });
            jest.useRealTimers();
        });
    });

    describe('go to logout', () => {
        beforeEach(() => {
            const mockDate = new Date(1466424490000);
            jest.useFakeTimers().setSystemTime(mockDate);
        });

        it('should add the cache busting', () => {
            service.doLogOut();
            expect(router.navigate).toHaveBeenCalledWith(['/dotAdmin/logout'], {
                queryParams: { r: 1466424490000 }
            });
            jest.useRealTimers();
        });
    });
});
