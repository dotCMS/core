import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { InputTextModule } from 'primeng/inputtext';

import { DotContentSearchService, DotLanguagesService } from '@dotcms/data-access';

import { DotAssetCardComponent } from './components/dot-asset-card/dot-asset-card.component';
import { DotAssetCardListComponent } from './components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetCardSkeletonComponent } from './components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';
import { DotAssetSearchComponent } from './dot-asset-search.component';
import { DotAssetSearchStore } from './store/dot-asset-search.store';

describe('DotAssetSearchComponent', () => {
    let spectator: Spectator<DotAssetSearchComponent>;
    let component: DotAssetSearchComponent;

    const createComponent = createComponentFactory({
        component: DotAssetSearchComponent,
        mocks: [DotContentSearchService, DotLanguagesService],
        providers: [DotAssetSearchStore, DotContentSearchService, DotLanguagesService],
        imports: [
            HttpClientTestingModule,
            InputTextModule,
            DotAssetCardComponent,
            DotAssetCardListComponent,
            DotAssetCardSkeletonComponent
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
