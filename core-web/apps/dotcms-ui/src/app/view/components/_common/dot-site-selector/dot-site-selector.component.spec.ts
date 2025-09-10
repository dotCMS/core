/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
    DotEventsService,
    DotMessageService,
    DotSystemConfigService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, Site, SiteService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotSiteSelectorComponent } from './dot-site-selector.component';

import { IframeOverlayService } from '../iframe/service/iframe-overlay.service';
import { SearchableDropdownComponent } from '../searchable-dropdown/component/searchable-dropdown.component';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';

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

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-site-selector [id]="id" [cssClass]="cssClass"></dot-site-selector>
    `,
    standalone: false
})
class TestHostComponent {
    @Input() id: string;
    @Input() cssClass: string;
}

describe('SiteSelectorComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let comp: DotSiteSelectorComponent;
    let deHost: DebugElement;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let siteService: SiteService;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            search: 'Search'
        });
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotSiteSelectorComponent],
            imports: [
                SearchableDropDownModule,
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
    }));

    it('should send notification when login-as/logout-as', fakeAsync(() => {
        const dotEventsService = de.injector.get(DotEventsService);
        jest.spyOn(comp, 'getSitesList');
        jest.spyOn(comp, 'handleSitesRefresh');
        fixtureHost.detectChanges();
        dotEventsService.notify('login-as');
        tick(0);
        dotEventsService.notify('logout-ass');
        tick(0);
        expect(comp.getSitesList).toHaveBeenCalledTimes(2);
    }));

    it('should set extra params to paginator service to false', () => {
        comp.archive = false;
        comp.live = false;
        comp.system = false;

        fixtureHost.detectChanges();

        expect(paginatorService.extraParams.get('archive')).toBe('false');
        expect(paginatorService.extraParams.get('live')).toBe('false');
        expect(paginatorService.extraParams.get('system')).toBe('false');
        expect(paginatorService.paginationPerPage).toBe(15);
    });

    it('should set extra params to paginator service to true', () => {
        comp.archive = true;
        comp.live = true;
        comp.system = true;

        fixtureHost.detectChanges();

        expect(paginatorService.extraParams.get('archive')).toBe('true');
        expect(paginatorService.extraParams.get('live')).toBe('true');
        expect(paginatorService.extraParams.get('system')).toBe('true');
    });

    it('should call getSitesList', () => {
        jest.spyOn<any>(siteService, 'switchSite$').mockReturnValue(observableOf(sites[0]));
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));

        fixtureHost.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
    });

    it('should call refresh if a event happen', () => {
        jest.spyOn<any>(siteService, 'refreshSites$').mockReturnValue(observableOf(sites[0]));
        jest.spyOn(comp, 'handleSitesRefresh');

        fixtureHost.detectChanges();

        expect(comp.handleSitesRefresh).toHaveBeenCalledTimes(1);
    });

    it('should change page', () => {
        const filter = 'filter';
        const page = 1;

        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));
        jest.spyOn<any>(siteService, 'switchSite$').mockReturnValue(observableOf({}));

        fixtureHost.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.pageChange.emit({
            filter: filter,
            first: 10,
            page: page,
            pageCount: 10,
            rows: 0
        });

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
    });

    it('should paginate when the filter change', () => {
        const filter = 'filter';

        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));
        jest.spyOn<any>(siteService, 'switchSite$').mockReturnValue(observableOf({}));

        fixtureHost.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.filterChange.emit(filter);
        comp.handleFilterChange(filter);

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(`*${filter}`);
    });

    it('should pass class name to searchable dropdown', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));
        componentHost.cssClass = 'hello';
        fixtureHost.detectChanges();

        const dropdown = de.query(By.css('dot-searchable-dropdown'));
        expect(dropdown.classes.hello).toBe(true);
    });

    it('should be assign to filter if empty', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));

        fixtureHost.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.filterChange.emit('');

        expect(paginatorService.filter).toEqual('*');
    });

    it('should emit switch event', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));
        jest.spyOn(comp, 'handleSitesRefresh');
        fixtureHost.detectChanges();
        const searchableDropdownComponent: DebugElement = de.query(
            By.css('dot-searchable-dropdown')
        );
        let result: any;
        comp.switch.subscribe((res) => (result = res));
        searchableDropdownComponent.triggerEventHandler('switch', { fake: 'site' });

        expect(result).toEqual({ fake: 'site' });
    });

    it('should set current site correctly', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        jest.spyOn(siteService, 'getSiteById').mockReturnValue(observableOf(mockSites[1]));
        jest.spyOn(paginatorService, 'getCurrentPage').mockReturnValue(observableOf(mockSites));
        fixtureHost.detectChanges();
        expect(comp.$currentSite()).toEqual(siteService.currentSite);
    });

    it('should set site based on passed id', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(mockSites));
        jest.spyOn(siteService, 'getSiteById').mockReturnValue(observableOf(mockSites[1]));
        jest.spyOn(siteService, 'currentSite', 'get', 'get').mockReturnValue(mockSites[0]);
        fixtureHost.detectChanges();
        componentHost.id = mockSites[1].identifier;
        fixtureHost.detectChanges();
        expect(comp.$currentSite()).toEqual(mockSites[1]);
    });

    describe('sitesCurrentPage', () => {
        const mockFunction = (times, success, fail) => {
            let count = 0;

            return Observable.create((observer) => {
                if (count++ >= times) {
                    observer.next(success);
                } else {
                    observer.next(fail);
                }
            });
        };

        it('should update until site is not present after archived', fakeAsync(() => {
            jest.spyOn(paginatorService, 'getCurrentPage').mockImplementation(() =>
                mockFunction(2, sites.slice(0, 2), sites)
            );
            comp.handleSitesRefresh(sites[2]);
            tick(2500);
            expect(paginatorService.getCurrentPage).toHaveBeenCalledTimes(1);
            expect(comp.$sitesCurrentPage()).toEqual(sites.slice(0, 2));
        }));

        it('should update until site is present after add', fakeAsync(() => {
            const subSites = sites.slice(0, 2);
            jest.spyOn(paginatorService, 'getCurrentPage').mockImplementation(() =>
                mockFunction(3, subSites, [])
            );
            comp.handleSitesRefresh(sites[0]);
            tick(3500);
            expect(comp.$sitesCurrentPage()).toEqual(subSites);
        }));
    });

    it('should display as field', () => {
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(sites));
        fixtureHost.detectChanges();
        const searchableDropdownComponent: DebugElement = de.query(
            By.css('dot-searchable-dropdown')
        );
        expect(searchableDropdownComponent).not.toBeNull();
    });

    it('should display only one result as field', () => {
        comp.asField = true;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf([sites[0]]));
        fixtureHost.detectChanges();
        const searchableDropdownComponent: DebugElement = de.query(
            By.css('dot-searchable-dropdown')
        );
        expect(searchableDropdownComponent).not.toBeNull();
    });

    it('should display as text if only one result', () => {
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf([sites[0]]));
        fixtureHost.detectChanges();
        const siteTitle: DebugElement = de.query(By.css('.site-selector__title'));
        expect(siteTitle).not.toBeNull();
    });
});
