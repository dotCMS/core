/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, input } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
    DotEventsService,
    DotMessageService,
    DotSystemConfigService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, LoggerService, Site, SiteService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotSiteSelectorComponent } from './dot-site-selector.component';

import { IframeOverlayService } from '../iframe/service/iframe-overlay.service';

const sites: Site[] = [
    {
        identifier: '1',
        hostname: 'Site 1',
        archived: false,
        type: 'host'
    },
    {
        identifier: '2',
        hostname: 'Site 2',
        archived: false,
        type: 'host'
    },
    {
        identifier: '3',
        hostname: 'Site 3',
        archived: true,
        type: 'host'
    }
];

const mockSystemConfig: DotSystemConfig = {
    logos: {
        loginScreen: '',
        navBar: ''
    },
    colors: {
        primary: '#54428e',
        secondary: '#3a3847',
        background: '#BB30E1'
    },
    releaseInfo: {
        buildDate: 'June 24, 2019',
        version: '5.0.0'
    },
    systemTimezone: {
        id: 'America/Costa_Rica',
        label: 'Costa Rica',
        offset: 360
    },
    languages: [],
    license: {
        level: 100,
        displayServerId: '19fc0e44',
        levelName: 'COMMUNITY EDITION',
        isCommunity: true
    },
    cluster: {
        clusterId: 'test-cluster',
        companyKeyDigest: 'test-digest'
    }
};

class MockDotSystemConfigService {
    getSystemConfig(): Observable<DotSystemConfig> {
        return observableOf(mockSystemConfig);
    }
}

class MockGlobalStore {
    setCurrentSite = jest.fn();
}

class MockLoggerService {
    warn = jest.fn();
    error = jest.fn();
    info = jest.fn();
    debug = jest.fn();
}

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-site-selector
            [id]="id"
            [cssClass]="cssClass"
            [archive]="archive"
            [live]="live"
            [system]="system"
            [asField]="asField"
            [width]="width"></dot-site-selector>
    `,
    standalone: true,
    imports: [DotSiteSelectorComponent]
})
class TestHostComponent {
    id: string;
    cssClass: string;
    archive: boolean;
    live: boolean;
    system: boolean;
    asField: boolean;
    width: string;
}

describe('DotSiteSelectorComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let comp: DotSiteSelectorComponent;
    let deHost: DebugElement;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let siteService: SiteService;
    let globalStore: MockGlobalStore;
    let loggerService: MockLoggerService;
    let dotEventsService: DotEventsService;
    const siteServiceMock = new SiteServiceMock();
    const refreshSitesSubject = new Subject<Site>();

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            search: 'Search'
        });
        globalStore = new MockGlobalStore();
        loggerService = new MockLoggerService();

        // Setup refreshSites$ observable BEFORE creating component
        Object.defineProperty(siteServiceMock, 'refreshSites$', {
            value: refreshSitesSubject.asObservable(),
            writable: true,
            configurable: true
        });

        // Setup currentSite$ observable BEFORE creating component
        Object.defineProperty(siteServiceMock, 'currentSite$', {
            value: observableOf(sites[0]),
            writable: true,
            configurable: true
        });

        // Mock PaginatorService.prototype.getWithOffset BEFORE creating component
        // because component has its own PaginatorService provider and constructor
        // calls loadAllSites() immediately. Each test can override this mock.
        // Use mockClear to ensure clean state
        const mockFn = PaginatorService.prototype.getWithOffset as jest.Mock;
        if (jest.isMockFunction(mockFn)) {
            mockFn.mockClear();
        }
        jest.spyOn(PaginatorService.prototype, 'getWithOffset').mockReturnValue(observableOf(sites));

        TestBed.configureTestingModule({
            imports: [
                TestHostComponent,
                DotSiteSelectorComponent,
                BrowserAnimationsModule,
                HttpClientTestingModule,
                CommonModule,
                FormsModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotSystemConfigService, useClass: MockDotSystemConfigService },
                { provide: GlobalStore, useValue: globalStore },
                { provide: LoggerService, useValue: loggerService },
                IframeOverlayService,
                PaginatorService,
                DotEventsService
            ]
        }).compileComponents();

        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;

        de = deHost.query(By.css('dot-site-selector'));
        comp = de.componentInstance;

        paginatorService = de.injector.get(PaginatorService);
        siteService = de.injector.get(SiteService);
        dotEventsService = de.injector.get(DotEventsService);
    }));

    afterEach(() => {
        // Clear all mocks
        jest.clearAllMocks();
        // Restore the prototype mock to default for next test
        // This ensures tests don't interfere with each other
        const mockFn = PaginatorService.prototype.getWithOffset as jest.Mock;
        if (jest.isMockFunction(mockFn)) {
            mockFn.mockClear();
            mockFn.mockReturnValue(observableOf(sites));
        }
        // Don't complete the subject, just clear any existing subscriptions
        // The subject will be reused across tests
    });

    describe('Initialization', () => {
        it('should initialize component', () => {
            expect(comp).toBeTruthy();
        });

        it('should initialize pagination service with correct values', () => {
            expect(paginatorService.url).toBe('v1/site');
            expect(paginatorService.paginationPerPage).toBe(1000);
        });

        it('should load sites on init', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            expect(PaginatorService.prototype.getWithOffset).toHaveBeenCalledWith(0);
            expect(comp.$sitesList().length).toBe(sites.length);
        }));
    });

    describe('Signal Inputs', () => {
        it('should set extra params to paginator service when archive input is false', () => {
            // Use default mock from beforeEach
            componentHost.archive = false;
            fixtureHost.detectChanges();
            expect(paginatorService.extraParams.get('archive')).toBe('false');
        });

        it('should set extra params to paginator service when inputs are true', () => {
            // Use default mock from beforeEach
            componentHost.archive = true;
            componentHost.live = true;
            componentHost.system = true;
            fixtureHost.detectChanges();
            expect(paginatorService.extraParams.get('archive')).toBe('true');
            expect(paginatorService.extraParams.get('live')).toBe('true');
            expect(paginatorService.extraParams.get('system')).toBe('true');
        });
    });

    describe('Site Loading', () => {
        it('should load all sites without pagination', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            expect(paginatorService.filter).toBe('*');
            expect(PaginatorService.prototype.getWithOffset).toHaveBeenCalledWith(0);
            expect(comp.$sitesList().length).toBe(sites.length);
        }));

        it('should refresh sites when refreshSites$ emits', fakeAsync(() => {
            // Use default mock from beforeEach
            jest.spyOn(comp, 'loadAllSites');
            fixtureHost.detectChanges();
            tick(); // Wait for initial load
            refreshSitesSubject.next(sites[0]);
            tick(); // Wait for refresh subscription
            expect(comp.loadAllSites).toHaveBeenCalled();
        }));

        it('should reload sites on login-as event', fakeAsync(() => {
            // Use default mock from beforeEach
            jest.spyOn(comp, 'loadAllSites');
            fixtureHost.detectChanges();
            dotEventsService.notify('login-as');
            tick(0);
            expect(comp.loadAllSites).toHaveBeenCalled();
        }));

        it('should reload sites on logout-as event', fakeAsync(() => {
            // Use default mock from beforeEach
            jest.spyOn(comp, 'loadAllSites');
            fixtureHost.detectChanges();
            dotEventsService.notify('logout-as');
            tick(0);
            expect(comp.loadAllSites).toHaveBeenCalled();
        }));
    });

    describe('Computed Signals', () => {
        it('should compute $moreThanOneSite correctly', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            expect(comp.$moreThanOneSite()).toBe(true);

            // Change mock for second part of test
            (PaginatorService.prototype.getWithOffset as jest.Mock).mockReturnValue(observableOf([sites[0]]));
            comp.loadAllSites();
            tick(); // Wait for observable to complete
            fixtureHost.detectChanges();
            expect(comp.$moreThanOneSite()).toBe(false);
        }));

        it('should compute $targetSite based on user selection priority', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for initial load
            comp.siteChange(sites[1]);
            tick(); // Wait for effects to run
            fixtureHost.detectChanges();
            expect(comp.$targetSite()?.identifier).toBe(sites[1].identifier);
        }));

        it('should compute $targetSite based on id input priority', fakeAsync(() => {
            // Use default mock from beforeEach
            componentHost.id = sites[1].identifier;
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete and effects to run
            expect(comp.$targetSite()?.identifier).toBe(sites[1].identifier);
        }));

        it('should compute $targetSite based on service site priority', fakeAsync(() => {
            Object.defineProperty(siteServiceMock, 'currentSite$', {
                value: observableOf(sites[0]),
                writable: true,
                configurable: true
            });
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete and effects to run
            // Service site should be used when no user selection or id input
            expect(comp.$targetSite()?.identifier).toBe(sites[0].identifier);
        }));
    });

    describe('Site Selection', () => {
        it('should emit switch event when site changes', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for initial load
            let result: Site | undefined;
            comp.switch.subscribe((site) => (result = site));
            comp.siteChange(sites[1]);
            tick(); // Wait for effects
            expect(result).toEqual(sites[1]);
            expect(globalStore.setCurrentSite).toHaveBeenCalledWith(sites[1]);
        }));

        it('should update current site when updateCurrentSite is called', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for initial load
            comp.updateCurrentSite(sites[1]);
            tick(); // Wait for effects to run
            fixtureHost.detectChanges();
            expect(comp.$currentSite()?.identifier).toBe(sites[1].identifier);
        }));

        it('should handle onSiteChange event', fakeAsync(() => {
            // Use default mock from beforeEach
            jest.spyOn(comp, 'siteChange');
            fixtureHost.detectChanges();
            tick(); // Wait for initial load
            comp.onSiteChange({ value: sites[1] });
            expect(comp.siteChange).toHaveBeenCalledWith(sites[1]);
        }));

        it('should set current site based on passed id', fakeAsync(() => {
            // Change mock for this test
            (PaginatorService.prototype.getWithOffset as jest.Mock).mockReturnValue(observableOf(mockSites));
            // Set id before detectChanges so effect can pick it up
            componentHost.id = mockSites[1].identifier;
            // Reload sites to trigger the effect
            comp.loadAllSites();
            tick(); // Wait for observable to complete
            fixtureHost.detectChanges(); // Trigger effects to sync $currentSite from $targetSite
            tick(); // Wait for effects
            fixtureHost.detectChanges(); // Final update
            expect(comp.$currentSite()?.identifier).toBe(mockSites[1].identifier);
        }));
    });

    describe('Missing Site ID Handling', () => {
        it('should retry loading when site is not found in list', fakeAsync(() => {
            const sitesWithoutTarget = [sites[0], sites[2]];

            // Override prototype mock to return sites without target
            const mockGetWithOffset = jest.fn().mockReturnValue(
                observableOf(sitesWithoutTarget)
            );
            PaginatorService.prototype.getWithOffset = mockGetWithOffset;

            // Set id to a site that's not in the list
            componentHost.id = sites[1].identifier;
            fixtureHost.detectChanges(); // Ensure id input is set
            comp.loadAllSites();
            tick(); // Wait for load and handleMissingSiteId to execute

            // Verify getWithOffset was called twice:
            // 1. First call from loadAllSites()
            // 2. Second call from #handleMissingSiteId() when it detects site is missing
            expect(mockGetWithOffset).toHaveBeenCalledTimes(2);
            expect(mockGetWithOffset).toHaveBeenNthCalledWith(1, 0);
            expect(mockGetWithOffset).toHaveBeenNthCalledWith(2, 0);

            // Verify warning was called
            expect(loggerService.warn).toHaveBeenCalledWith(
                expect.stringContaining(`Site with ID ${sites[1].identifier} not found in list`)
            );
            expect(comp.$sitesList().length).toBe(2);
        }));

        it('should not retry if site is already in list', fakeAsync(() => {
            // Use default mock from beforeEach
            jest.spyOn(loggerService, 'warn');
            componentHost.id = sites[0].identifier;
            // Reload to check if site is missing (it shouldn't be)
            comp.loadAllSites();
            tick(); // Wait for observable to complete
            expect(loggerService.warn).not.toHaveBeenCalled();
        }));
    });

    describe('Template Rendering', () => {
        it('should display p-select when more than one site', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            const select = de.query(By.css('p-select'));
            expect(select).not.toBeNull();
        }));

        it('should display p-select when asField is true', fakeAsync(() => {
            (PaginatorService.prototype.getWithOffset as jest.Mock).mockReturnValue(observableOf([sites[0]]));
            componentHost.asField = true;
            comp.loadAllSites();
            tick(); // Wait for observable to complete
            fixtureHost.detectChanges();
            const select = de.query(By.css('p-select'));
            expect(select).not.toBeNull();
        }));

        it('should display text when only one site and not asField', fakeAsync(() => {
            const singleSite = [sites[0]];
            // Ensure currentSite$ returns the single site so $targetSite can find it
            Object.defineProperty(siteServiceMock, 'currentSite$', {
                value: observableOf(sites[0]),
                writable: true,
                configurable: true
            });
            (PaginatorService.prototype.getWithOffset as jest.Mock).mockReturnValue(observableOf(singleSite));
            // Reload to get single site
            comp.loadAllSites();
            tick(); // Wait for observable to complete
            fixtureHost.detectChanges(); // Trigger effects to sync $currentSite from $targetSite
            tick(); // Wait for effects
            fixtureHost.detectChanges(); // Update template
            const siteTitle = de.query(By.css('.site-selector__title'));
            expect(siteTitle).not.toBeNull();
            expect(siteTitle.nativeElement.textContent.trim()).toBe('Site 1');
        }));

        it('should pass class name to p-select', fakeAsync(() => {
            // Use default mock from beforeEach
            componentHost.cssClass = 'hello';
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            const select = de.query(By.css('p-select'));
            expect(select.nativeElement.classList.contains('hello')).toBe(true);
        }));
    });

    describe('Events', () => {
        it('should emit display event when select is shown', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            let displayEmitted = false;
            comp.display.subscribe(() => (displayEmitted = true));
            comp.onSelectShow();
            expect(displayEmitted).toBe(true);
        }));

        it('should emit hide event when select is hidden', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            let hideEmitted = false;
            comp.hide.subscribe(() => (hideEmitted = true));
            const select = de.query(By.css('p-select'));
            select.triggerEventHandler('onHide', {});
            expect(hideEmitted).toBe(true);
        }));
    });

    describe('Virtual Scrolling', () => {
        it('should have virtual scroll options configured', () => {
            const options = comp.$virtualScrollOptions();
            expect(options.scrollHeight).toBe('300px');
            expect(options.autoSize).toBe(true);
        });

        it('should reset virtual scroller state on show', fakeAsync(() => {
            // Use default mock from beforeEach
            fixtureHost.detectChanges();
            tick(); // Wait for observable to complete
            
            const setInitialStateSpy = jest.fn();
            comp.select = {
                scroller: {
                    setInitialState: setInitialStateSpy
                }
            } as any;
            
            // Mock requestAnimationFrame to execute immediately
            const originalRAF = window.requestAnimationFrame;
            window.requestAnimationFrame = (callback: FrameRequestCallback) => {
                callback(0);
                return 0;
            };
            
            comp.onSelectShow();
            
            // Restore original
            window.requestAnimationFrame = originalRAF;
            
            expect(setInitialStateSpy).toHaveBeenCalled();
        }));
    });
});
