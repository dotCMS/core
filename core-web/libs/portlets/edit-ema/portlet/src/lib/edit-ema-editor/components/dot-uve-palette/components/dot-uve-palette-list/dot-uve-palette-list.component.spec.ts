import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { MessageService } from 'primeng/api';

import {
    DotESContentService,
    DotLocalstorageService,
    DotMessageService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUvePaletteListComponent } from './dot-uve-palette-list.component';

import { DotUVEPaletteListTypes } from '../../models';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';

describe('DotUvePaletteListComponent', () => {
    let spectator: Spectator<DotUvePaletteListComponent>;

    const mockDotPageContentTypeService = {
        get: jest.fn().mockReturnValue(
            of({
                contenttypes: [],
                pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
            })
        ),
        getAllContentTypes: jest.fn().mockReturnValue(
            of({
                contenttypes: [],
                pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
            })
        )
    };

    const mockDotPageFavoriteContentTypeService = {
        getAll: jest.fn().mockReturnValue([]),
        isFavorite: jest.fn().mockReturnValue(false),
        add: jest.fn().mockReturnValue([]),
        remove: jest.fn().mockReturnValue([]),
        set: jest.fn().mockReturnValue([])
    };

    const mockDotESContentService = {
        get: jest.fn().mockReturnValue(
            of({
                contentlets: [],
                pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
            })
        )
    };

    const createComponent = createComponentFactory({
        component: DotUvePaletteListComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotPageContentTypeService,
                useValue: mockDotPageContentTypeService
            },
            {
                provide: DotPageFavoriteContentTypeService,
                useValue: mockDotPageFavoriteContentTypeService
            },
            DotLocalstorageService,
            {
                provide: DotESContentService,
                useValue: mockDotESContentService
            },
            MessageService,
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            },
            {
                provide: DotMessageService,
                useValue: {
                    ...MockDotMessageService,
                    get: (key: string) => key
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        // Use componentRef.setInput for required signal inputs with aliases
        spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
        spectator.fixture.componentRef.setInput('languageId', 1);
        spectator.fixture.componentRef.setInput('pagePath', '/test-page');
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
