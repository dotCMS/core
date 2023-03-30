import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { dotEventSocketURLFactory } from '@dotcms/app/test/dot-test-bed';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { LoginServiceMock, mockAuth } from '@dotcms/utils-testing';

import { DotToolbarUserStore } from './dot-toolbar-user.store';

describe('DotToolbarUserStore', () => {
    let store: DotToolbarUserStore;
    let loginService: LoginService;
    let locationService: Location;
    let dotNavigationService: DotNavigationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule, RouterTestingModule],
            providers: [
                DotToolbarUserStore,
                LoggerService,
                DotMessageService,
                DotNavigationService,
                DotEventsService,
                DotIframeService,
                DotMenuService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                StringUtils,
                DotRouterService,
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {
                            return;
                        }
                    }
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                { provide: LoginService, useClass: LoginServiceMock }
            ]
        });

        store = TestBed.inject(DotToolbarUserStore);
        loginService = TestBed.inject(LoginService);
        locationService = TestBed.inject(LOCATION_TOKEN);
        dotNavigationService = TestBed.inject(DotNavigationService);
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    it('should set the initial state when init is called', () => {
        store.init();

        store.state$.subscribe((state) => {
            const { items, userData, showLoginAs, showMyAccount } = state;

            expect(items.length).toBeTruthy();
            expect(userData).toEqual({
                email: mockAuth.loginAsUser.emailAddress,
                name: mockAuth.loginAsUser.name
            });
            expect(showLoginAs).toBeFalse();
            expect(showMyAccount).toBeFalse();
        });
    });

    it('should trigger loginService logoutAs, navigate to first portlet and reload the page when logoutAs is called', fakeAsync(() => {
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );

        spyOn(loginService, 'logoutAs').and.callThrough();
        spyOn(locationService, 'reload');

        store.logoutAs();

        expect(loginService.logoutAs).toHaveBeenCalled();
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();

        tick();
        expect(locationService.reload).toHaveBeenCalled();
    }));

    describe('showLoginAs method', () => {
        it('should change its state value to true', () => {
            store.showLoginAs(true);
            store.state$.subscribe((state) => {
                expect(state.showLoginAs).toBeTrue();
            });
        });

        it('should change its state value to false', () => {
            store.showLoginAs(false);
            store.state$.subscribe((state) => {
                expect(state.showLoginAs).toBeFalse();
            });
        });
    });

    describe('showMyAccount method', () => {
        it('should change its state value to true', () => {
            store.showMyAccount(true);
            store.state$.subscribe((state) => {
                expect(state.showMyAccount).toBeTrue();
            });
        });

        it('should change its state value to false', () => {
            store.showMyAccount(false);
            store.state$.subscribe((state) => {
                expect(state.showMyAccount).toBeFalse();
            });
        });
    });
});
