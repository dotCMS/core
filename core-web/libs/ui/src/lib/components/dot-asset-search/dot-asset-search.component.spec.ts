import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';

import { InputTextModule } from 'primeng/inputtext';

import { DotContentSearchService, DotLanguagesService } from '@dotcms/data-access';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotAssetCardComponent } from './components/dot-asset-card/dot-asset-card.component';
import { DotAssetCardListComponent } from './components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetCardSkeletonComponent } from './components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';
import { DotAssetSearchComponent } from './dot-asset-search.component';
import { DotAssetSearchStore } from './store/dot-asset-search.store';

describe('DotAssetSearchComponent', () => {
    let spectator: Spectator<DotAssetSearchComponent>;

    let store: DotAssetSearchStore;

    const createComponent = createComponentFactory({
        component: DotAssetSearchComponent,
        providers: [
            {
                provide: DotContentSearchService,
                useValue: {
                    get: () => of({ jsonObjectView: { contentlets: [] } })
                }
            },
            {
                provide: DotLanguagesService,
                useValue: {
                    get: () =>
                        of([
                            {
                                id: '1',
                                languageCode: 'en',
                                countryCode: 'us',
                                language: 'English',
                                country: 'United States'
                            }
                        ])
                }
            }
        ],
        imports: [
            HttpClientTestingModule,
            InputTextModule,
            DotAssetCardComponent,
            DotAssetCardListComponent,
            DotAssetCardSkeletonComponent
        ],
        componentProviders: [DotAssetSearchStore]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                languageId: '1',
                type: 'image'
            }
        });
        spectator.detectChanges();
        store = spectator.inject(DotAssetSearchStore, true);
        spectator.detectChanges();
    });

    it('should send the correct inputs to DotAssetCardListComponent', () => {
        const dotAssetCardListComponent = spectator.query(DotAssetCardListComponent);

        // Default state
        expect(dotAssetCardListComponent.contentlets).toEqual([]);
        expect(dotAssetCardListComponent.done).toEqual(true);
        expect(dotAssetCardListComponent.loading).toEqual(false);
    });

    it('should call store nextBatch', fakeAsync(() => {
        const spy = spyOn(store, 'nextBatch');
        spectator.triggerEventHandler(DotAssetCardListComponent, 'nextBatch', 10);
        tick(1000);
        expect(spy).toHaveBeenCalledWith({
            languageId: '1',
            assetType: 'image',
            offset: 10,
            search: ''
        });
    }));

    it('should call addAsset Output', fakeAsync(() => {
        const spy = spyOn(spectator.component.addAsset, 'emit');
        spectator.triggerEventHandler(DotAssetCardListComponent, 'selectedItem', EMPTY_CONTENTLET);
        tick(1000);
        expect(spy).toHaveBeenCalledWith(EMPTY_CONTENTLET);
    }));

    it('should call store searchContentlet', fakeAsync(() => {
        const spy = spyOn(store, 'searchContentlet');
        const inputElement = spectator.query(byTestId('input-search')) as HTMLInputElement;
        spectator.typeInElement('search', inputElement);
        tick(1000);
        expect(spy).toHaveBeenCalledWith({
            languageId: '1',
            assetType: 'image',
            offset: 0,
            search: 'search'
        });
    }));
});
