import { createDirectiveFactory, SpectatorDirective } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Select, SelectFilterEvent, SelectModule } from 'primeng/select';

import { DotEventsService, DotSiteService } from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotPagination, DotSite } from '@dotcms/dotcms-models';
import { CoreWebServiceMock, SiteServiceMock } from '@dotcms/utils-testing';

import { DotSiteSelectorDirective } from './dot-site-selector.directive';

const mockSites: DotSite[] = [
    {
        hostname: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        archived: false,
        aliases: null
    },
    {
        hostname: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        archived: false,
        aliases: null
    }
];

const mockPagination: DotPagination = {
    currentPage: 1,
    perPage: 10,
    totalEntries: 2
};

describe('DotSiteSelectorDirective', () => {
    let spectator: SpectatorDirective<DotSiteSelectorDirective>;
    let dropdown: Select;
    let dotEventsService: DotEventsService;
    let dotSiteService: DotSiteService;

    const createDirective = createDirectiveFactory({
        directive: DotSiteSelectorDirective,
        imports: [HttpClientTestingModule, SelectModule, BrowserAnimationsModule],
        providers: [
            { provide: SiteService, useClass: SiteServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            DotEventsService,
            DotSiteService
        ]
    });

    beforeEach(() => {
        spectator = createDirective(
            '<p-select [dotSiteSelector] [archive]="true" [live]="true" [system]="true" [pageSize]="10"></p-select>'
        );
        dotSiteService = spectator.inject(DotSiteService);
        dotEventsService = spectator.inject(DotEventsService);
        dropdown = spectator.directive['control'] as Select;
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.directive).toBeTruthy();
    });

    describe('Get Sites', () => {
        let getSitesSpy;

        beforeEach(() => {
            getSitesSpy = jest
                .spyOn(dotSiteService, 'getSites')
                .mockReturnValue(of({ sites: mockSites, pagination: mockPagination }));
        });

        it('should get sites list', () => {
            spectator.detectChanges();

            spectator.directive.ngOnInit();

            expect(getSitesSpy).toHaveBeenCalled();
        });

        it('should get sites list with filter', fakeAsync(() => {
            const event: SelectFilterEvent = {
                filter: 'demo',
                originalEvent: new MouseEvent('click')
            };
            dropdown.onFilter.emit(event);

            spectator.tick(500);
            expect(dotSiteService.getSites).toHaveBeenCalledWith({
                filter: event.filter,
                per_page: 10
            });
        }));
    });

    describe('Listen login-as/logout-as events', () => {
        it('should send notification when login-as/logout-as', fakeAsync(() => {
            const getSitesSpy = jest
                .spyOn(dotSiteService, 'getSites')
                .mockReturnValue(of({ sites: mockSites, pagination: mockPagination }));
            spectator.detectChanges();
            dotEventsService.notify('login-as');
            spectator.tick(0);
            dotEventsService.notify('logout-as');
            spectator.tick(0);
            expect(getSitesSpy).toHaveBeenCalledTimes(2);
        }));
    });
});
