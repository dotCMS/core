import { DotRouterService } from './dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginService } from 'dotcms-js';
import { Router, ActivatedRoute } from '@angular/router';
import { async, TestBed } from '@angular/core/testing';

class RouterMock {
    url = '/c/test';

    routerState = {
        snapshot: {
            url: '/c/hello-world'
        }
    };

    navigate = jasmine.createSpy('navigate').and.callFake(() => {
        return new Promise((resolve) => {
            resolve(true);
        });
    });

    getCurrentNavigation() {
        return {
            finalUrl: {}
        };
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
    let router: Router;

    beforeEach(async(() => {
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

        service = testbed.get(DotRouterService);
        router = testbed.get(Router);
    }));

    it('should get queryParams from Router', () => {
        spyOn(router, 'getCurrentNavigation').and.returnValue({
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
        spyOn(router, 'getCurrentNavigation').and.returnValue(null);
        expect(service.queryParams).toEqual({
            hello: 'world'
        });
    });

    it('should go to main', () => {
        service.goToMain();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should go to edit page', () => {
        spyOn(service, 'goToEditPage');
        service.goToMain('/about/us');

        expect(service.goToEditPage).toHaveBeenCalledWith({ url: '/about/us' });
    });

    it('should go to edit content type page', () => {
        service.goToEditContentType('123', 'Form');

        expect(router.navigate).toHaveBeenCalledWith(['/Form/edit/123']);
    });

    it('should go to previousSavedURL', () => {
        service.previousSavedURL = 'test/fake';
        service.goToMain();

        expect(router.navigate).toHaveBeenCalledWith(['test/fake']);
    });

    it('should go to edit page', () => {
        service.goToEditPage({ url: 'abc/def' });
        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: { url: 'abc/def' }
        });
    });

    it('should go to edit page with language_id', () => {
        service.goToEditPage({ url: 'abc/def', language_id: '1' });
        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: { url: 'abc/def', language_id: '1' }
        });
    });

    it('should go to edit contentlet', () => {
        service.goToEditContentlet('123');
        expect(router.navigate).toHaveBeenCalledWith(['/c/hello-world/123']);
    });

    it('should go to edit workflow task', () => {
        service.goToEditTask('123');
        expect(router.navigate).toHaveBeenCalledWith(['/c/workflow/123']);
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
        const params = {id: 'content'};
        service.replaceQueryParams(params);
        expect(router.navigate).toHaveBeenCalledWith([],
            {
                queryParams: params,
                queryParamsHandling: 'merge'
            });
    });
});
