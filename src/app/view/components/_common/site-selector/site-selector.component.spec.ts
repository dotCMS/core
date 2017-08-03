import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, ElementRef } from '@angular/core';
import { SiteSelectorComponent } from './site-selector.component';
import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import {
    OverlayPanelModule,
    ButtonModule,
    InputTextModule,
    PaginatorModule
} from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { SiteServiceMock } from '../../../../test/site-service.mock';
import { IframeOverlayService } from '../../../../api/services/iframe-overlay-service';
import { SiteService } from '../../../../api/services/site-service';
import { Observable } from 'rxjs/Observable';
import { DotcmsConfig } from '../../../../api/services/system/dotcms-config';
import { SearchableDropdownComponent } from '../searchable-dropdown/component/searchable-dropdown.component';
import { fakeAsync, tick } from '@angular/core/testing';
import { PaginatorService } from '../../../../api/services/paginator';

describe('Site Selector Component', () => {
    let comp: SiteSelectorComponent;
    let fixture: ComponentFixture<SiteSelectorComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(
        async(() => {
            let messageServiceMock = new MockMessageService({
                search: 'Search'
            });

            let siteServiceMock = new SiteServiceMock();

            DOTTestBed.configureTestingModule({
                declarations: [SiteSelectorComponent],
                imports: [SearchableDropDownModule],
                providers: [
                    { provide: MessageService, useValue: messageServiceMock },
                    { provide: SiteService, useValue: siteServiceMock },
                    IframeOverlayService,
                    PaginatorService
                ]
            });

            fixture = DOTTestBed.createComponent(SiteSelectorComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;
        })
    );

    it('should set extra params to paginator service', () => {
        comp.archive = true;
        comp.live = false;
        comp.system = false;

        fixture.detectChanges();

        let paginatorService = de.injector.get(PaginatorService);

        expect(paginatorService.extraParams.get('archive')).toBe('true');
        expect(paginatorService.extraParams.get('live')).toBe('false');
        expect(paginatorService.extraParams.get('system')).toBe('false');
    });

    it('should set extra params to paginator service (system default)', () => {
        comp.archive = true;
        comp.live = false;

        fixture.detectChanges();

        let paginatorService = de.injector.get(PaginatorService);

        expect(paginatorService.extraParams.get('archive')).toBe('true');
        expect(paginatorService.extraParams.get('live')).toBe('false');
        expect(paginatorService.extraParams.get('system')).toBe('true');
    });

    it('should call getSitesList', () => {
        let site1 = {
            identifier: 1,
            name: 'Site 1'
        };

        let site2 = {
            identifier: 2,
            name: 'Site 2'
        };

        let siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of(site1));

        let paginatorService = de.injector.get(PaginatorService);
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([site1, site2]));

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
    });

    it('should call refresh if a event happen', async(() => {
        let site1 = {
            identifier: 1,
            name: 'Site 1'
        };

        let site2 = {
            identifier: 2,
            name: 'Site 2'
        };

        let siteService = de.injector.get(SiteService);
        let spy = spyOn(siteService, 'refreshSites$').and.returnValue(
            Observable.of([site1, site2])
        );

        fixture.detectChanges();

        expect(spy.calls.any()).toEqual(false);
    }));

    it('should change Page', fakeAsync(() => {
        let filter = 'filter';
        let page = 1;

        let paginatorService = de.injector.get(PaginatorService);
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        let siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        let searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.pageChange.emit({
            filter: filter,
            first: 10,
            page: page,
            pageCount: 10,
            rows: 0
        });

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
    }));

    it('should paginate when the filter change', fakeAsync(() => {
        let filter = 'filter';
        let first = 2;

        let paginatorService = de.injector.get(PaginatorService);
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        let siteService = de.injector.get(SiteService);
        spyOn(siteService, 'switchSite$').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        let searchableDropdownComponent: SearchableDropdownComponent = de.query(
            By.css('searchable-dropdown')
        ).componentInstance;

        searchableDropdownComponent.filterChange.emit(filter);
        comp.handleFilterChange(filter);

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    }));
});
