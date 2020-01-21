import { Observable, of as observableOf } from 'rxjs';
import { ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotSiteSelectorComponent } from './dot-site-selector.component';
import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { SiteServiceMock, mockSites } from '../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js';
import { SearchableDropdownComponent } from '../searchable-dropdown/component/searchable-dropdown.component';
import { PaginatorService } from '@services/paginator';
import { IframeOverlayService } from '../iframe/service/iframe-overlay.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { Site } from '../../../../../../projects/dotcms-js/src/lib/core/site.service';

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

describe('SiteSelectorComponent', () => {
    let comp: DotSiteSelectorComponent;
    let fixture: ComponentFixture<DotSiteSelectorComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let siteService: SiteService;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(
        async(() => {
            const messageServiceMock = new MockDotMessageService({
                search: 'Search'
            });
            DOTTestBed.configureTestingModule({
                declarations: [DotSiteSelectorComponent],
                imports: [SearchableDropDownModule, BrowserAnimationsModule],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: SiteService, useValue: siteServiceMock },
                    IframeOverlayService,
                    PaginatorService
                ]
            });

            fixture = DOTTestBed.createComponent(DotSiteSelectorComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            paginatorService = de.injector.get(PaginatorService);
            siteService = de.injector.get(SiteService);
        })
    );

    it(
        'should send notification when login-as/logout-as',
        fakeAsync(() => {
            const dotEventsService = de.injector.get(DotEventsService);
            spyOn(comp, 'getSitesList');
            spyOn(comp, 'handleSitesRefresh');
            fixture.detectChanges();
            dotEventsService.notify('login-as');
            tick(0);
            dotEventsService.notify('logout-ass');
            tick(0);
            expect(comp.getSitesList).toHaveBeenCalledTimes(2);
        })
    );

    it('should set extra params to paginator service to false', () => {
        comp.archive = false;
        comp.live = false;
        comp.system = false;

        fixture.detectChanges();

        expect(paginatorService.extraParams.get('archive')).toBe('false');
        expect(paginatorService.extraParams.get('live')).toBe('false');
        expect(paginatorService.extraParams.get('system')).toBe('false');
    });

    it('should set extra params to paginator service to true', () => {
        comp.archive = true;
        comp.live = true;
        comp.system = true;

        fixture.detectChanges();

        expect(paginatorService.extraParams.get('archive')).toBe('true');
        expect(paginatorService.extraParams.get('live')).toBe('true');
        expect(paginatorService.extraParams.get('system')).toBe('true');
    });

    it('should call getSitesList', () => {
        spyOn(siteService, 'switchSite$').and.returnValue(observableOf(sites[0]));
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf(sites));

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
    });

    it('should call refresh if a event happen', () => {
        spyOn(siteService, 'refreshSites$').and.returnValue(observableOf(sites[0]));
        spyOn(comp, 'handleSitesRefresh').and.callThrough();

        fixture.detectChanges();

        expect(comp.handleSitesRefresh).toHaveBeenCalledTimes(1);
    });

    describe('Pagination', () => {});

    it('should change page', () => {
        const filter = 'filter';
        const page = 1;

        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));
        spyOn(siteService, 'switchSite$').and.returnValue(observableOf({}));

        fixture.detectChanges();

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

        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));
        spyOn(siteService, 'switchSite$').and.returnValue(observableOf({}));

        fixture.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.filterChange.emit(filter);
        comp.handleFilterChange(filter);

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    });

    it('should be assign to filter if empty', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));

        fixture.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.filterChange.emit('');

        expect(paginatorService.filter).toEqual('');
    });

    it('should emit change event', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));
        spyOn(comp, 'handleSitesRefresh');
        fixture.detectChanges();
        const searchableDropdownComponent: DebugElement = de.query(
            By.css('dot-searchable-dropdown')
        );
        let result: any;
        comp.change.subscribe(res => (result = res));
        searchableDropdownComponent.triggerEventHandler('change', { fake: 'site' });

        expect(result).toEqual({ fake: 'site' });
    });

    it('should set current site correctly', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));
        spyOn(comp, 'handleSitesRefresh');
        fixture.detectChanges();

        let result: any;
        comp.currentSite.subscribe(res => (result = res));

        expect(result).toEqual(mockSites[0]);
    });

    it('should set current on switchSite$', () => {
        fixture.detectChanges();
        siteServiceMock.setFakeCurrentSite(sites[1]);

        comp.currentSite.subscribe(site => {
            expect(site).toEqual(sites[1]);
        });
    });

    describe('sitesCurrentPage', () => {
        const mockFunction = (times, success, fail) => {
            let count = 0;
            return Observable.create(observer => {
                if (count++ >= times) {
                    observer.next(success);
                } else {
                    observer.next(fail);
                }
            });
        };

        it(
            'should update until site is not present after archived',
            fakeAsync(() => {
                spyOn(paginatorService, 'getCurrentPage').and.callFake(() =>
                    mockFunction(2, sites.slice(0, 2), sites)
                );
                comp.handleSitesRefresh(sites[2]);
                tick(2500);
                expect(paginatorService.getCurrentPage).toHaveBeenCalledTimes(1);
                expect(comp.sitesCurrentPage).toEqual(sites.slice(0, 2));
            })
        );

        it(
            'should update until site is present after add',
            fakeAsync(() => {
                spyOn(siteService, 'getSiteById').and.callFake(() =>
                    mockFunction(2, sites[1], undefined)
                );
                spyOn(paginatorService, 'getCurrentPage').and.callThrough();
                comp.handleSitesRefresh(sites[0]);
                tick(2500);
                expect(siteService.getSiteById).toHaveBeenCalledWith(sites[0].identifier);
                expect(paginatorService.getCurrentPage).toHaveBeenCalledTimes(1);
            })
        );
    });
});
