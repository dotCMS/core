import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotESContentService, DotLocalstorageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';

import { DotFavoriteSelectorComponent } from './dot-favorite-selector.component';

import { DotPageContentTypeService } from '../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';
import { DotPaletteListStore } from '../dot-uve-palette-list/store/store';

describe('DotFavoriteSelectorComponent', () => {
    let spectator: Spectator<DotFavoriteSelectorComponent>;

    const createComponent = createComponentFactory({
        component: DotFavoriteSelectorComponent,
        imports: [HttpClientTestingModule],
        providers: [
            DotPaletteListStore,
            DotPageContentTypeService,
            DotPageFavoriteContentTypeService,
            DotLocalstorageService,
            DotESContentService,
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
