import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotEventsService,
    DotMessageService,
    DotRouterService,
    DotIframeService
} from '@dotcms/data-access';
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

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { LOCATION_TOKEN } from '../../../../../../providers';
import { dotEventSocketURLFactory } from '../../../../../../test/dot-test-bed';
import { DotNavigationService } from '../../../../dot-navigation/services/dot-navigation.service';

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
        jest.spyOn(dotNavigationService, 'goToFirstPortlet').mockReturnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );

        jest.spyOn(loginService, 'logoutAs');
        jest.spyOn(locationService, 'reload');

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
