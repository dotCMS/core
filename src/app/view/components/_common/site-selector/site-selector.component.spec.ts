import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { SiteSelectorComponent } from './site-selector.component';
import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { SiteServiceMock, mockSites } from '../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { SearchableDropdownComponent } from '../searchable-dropdown/component/searchable-dropdown.component';
import { PaginatorService } from '../../../../api/services/paginator';
import { IframeOverlayService } from '../iframe/service/iframe-overlay.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('SiteSelectorComponent', () => {
    let comp: SiteSelectorComponent;
    let fixture: ComponentFixture<SiteSelectorComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;

    beforeEach(
        async(() => {
            const messageServiceMock = new MockDotMessageService({
                search: 'Search'
            });

            const siteServiceMock = new SiteServiceMock();

            DOTTestBed.configureTestingModule({
                declarations: [SiteSelectorComponent],
                imports: [SearchableDropDownModule, BrowserAnimationsModule],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: SiteService, useValue: siteServiceMock },
                    IframeOverlayService,
                    PaginatorService
                ]
            });

            fixture = DOTTestBed.createComponent(SiteSelectorComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            paginatorService = de.injector.get(PaginatorService);
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
        const site1 = {
            identifier: 1,
            name: 'Site 1'
        };

        const site2 = {
            identifier: 2,
            name: 'Site 2'
        };

        const siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of(site1));

        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([site1, site2]));

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
    });

    it('should call refresh if a event happen', () => {
        const site1 = {
            identifier: 1,
            name: 'Site 1'
        };

        const site2 = {
            identifier: 2,
            name: 'Site 2'
        };

        const siteService = de.injector.get(SiteService);
        spyOn(siteService, 'refreshSites$').and.returnValue(Observable.of([site1, site2]));
        spyOn(comp, 'handleSitesRefresh').and.callThrough();

        fixture.detectChanges();

        expect(comp.handleSitesRefresh).toHaveBeenCalledTimes(1);
    });

    it('should change page', () => {
        const filter = 'filter';
        const page = 1;

        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        const siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(By.css('dot-searchable-dropdown')).componentInstance;

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
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        const siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(By.css('dot-searchable-dropdown')).componentInstance;

        searchableDropdownComponent.filterChange.emit(filter);
        comp.handleFilterChange(filter);

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    });

    it('should be assign to filter if empty', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        fixture.detectChanges();

        const searchableDropdownComponent: SearchableDropdownComponent = de.query(By.css('dot-searchable-dropdown')).componentInstance;

        searchableDropdownComponent.filterChange.emit('');

        expect(paginatorService.filter).toEqual('');
    });

    it('should emit change event', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        fixture.detectChanges();
        const searchableDropdownComponent: DebugElement = de.query(By.css('dot-searchable-dropdown'));
        let result: any;
        comp.change.subscribe(res => result = res);
        searchableDropdownComponent.triggerEventHandler('change',  {fake: 'site'});

        expect(result).toEqual({fake: 'site'});
    });

    it('should set current site correctly', () => {
        paginatorService.filter = 'filter';
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        fixture.detectChanges();

        let result: any;
        comp.currentSite.subscribe(res => result = res);

        expect(result).toEqual(mockSites[0]);
    });
});
