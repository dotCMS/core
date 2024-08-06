import { createDirectiveFactory, SpectatorDirective, mockProvider } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { DropdownFilterEvent } from 'primeng/dropdown/dropdown.interface';

import { DotEventsService, DotSiteService } from '@dotcms/data-access';
import { CoreWebService, mockSites, SiteService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, SiteServiceMock } from '@dotcms/utils-testing';

import { DotSiteSelectorDirective } from './dot-site-selector.directive';

describe('DotSiteSelectorDirective', () => {
    let spectator: SpectatorDirective<DotSiteSelectorDirective>;
    let dropdown: Dropdown;
    let dotEventsService: DotEventsService;
    let dotSiteService: DotSiteService;

    const createDirective = createDirectiveFactory({
        directive: DotSiteSelectorDirective,
        imports: [HttpClientTestingModule, DropdownModule, BrowserAnimationsModule],
        providers: [
            { provide: SiteService, useClass: SiteServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            DotEventsService,
            mockProvider(DotSiteService, {
                getSites: jasmine.createSpy().and.returnValue(of(mockSites))
            }),
        ]
    });

    beforeEach(() => {
        spectator = createDirective(
            '<p-dropdown [dotSiteSelector] [archive]="true" [live]="true" [system]="true" [pageSize]="10"></p-dropdown>'
        );
        dotSiteService = spectator.inject(DotSiteService);
        dotEventsService = spectator.inject(DotEventsService);
        dropdown = spectator.directive['control'] as Dropdown;
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.directive).toBeTruthy();
    });

    describe('Get Sites', () => {


        it('should get sites list', () => {
            spectator.detectChanges();

            spectator.directive.ngOnInit();

            expect(dotSiteService.getSites).toHaveBeenCalled();
        });

        it('should get sites list with filter', fakeAsync(() => {
            const event: DropdownFilterEvent = {
                filter: 'demo',
                originalEvent: new MouseEvent('test')
            };
            dropdown.onFilter.emit(event);

            spectator.tick(500);
            expect(dotSiteService.getSites).toHaveBeenCalledWith(event.filter, 10);
        }));
    });

    describe('Listen login-as/logout-as events', () => {
        it('should send notification when login-as/logout-as', fakeAsync(() => {
            spectator.detectChanges();
            dotEventsService.notify('login-as');
            spectator.tick(0);
            dotEventsService.notify('logout-as');
            spectator.tick(0);
            expect(dotSiteService.getSites).toHaveBeenCalledTimes(2);
        }));
    });
});
