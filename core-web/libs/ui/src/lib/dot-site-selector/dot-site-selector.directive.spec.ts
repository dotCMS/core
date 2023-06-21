import { createDirectiveFactory, SpectatorDirective } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DropdownModule } from 'primeng/dropdown';

import { DotEventsService, PaginatorService } from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, SiteServiceMock } from '@dotcms/utils-testing';

import { DotSiteSelectorDirective } from './dot-site-selector.directive';

describe('DotSiteSelectorDirective', () => {
    let spectator: SpectatorDirective<DotSiteSelectorDirective>;
    const createDirective = createDirectiveFactory({
        directive: DotSiteSelectorDirective,
        imports: [HttpClientTestingModule, DropdownModule],
        providers: [
            { provide: SiteService, useValue: new SiteServiceMock() },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            {
                provide: DotEventsService
            },
            {
                provide: PaginatorService
            }
        ]
    });

    beforeEach(() => {
        spectator = createDirective('<p-dropdown dotSiteSelector><p-dropdown>');
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.directive).toBeTruthy();
    });
});
