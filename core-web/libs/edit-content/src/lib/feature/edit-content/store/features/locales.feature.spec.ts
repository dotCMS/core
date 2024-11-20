import { SpyObject, createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotCMSContentlet } from '@dotcms/angular';
import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { contentInitialState } from '@dotcms/edit-content/feature/edit-content/store/features/content.feature';
import { withLocales } from '@dotcms/edit-content/feature/edit-content/store/features/locales.feature';

const MOCK_LANGUAGES = [
    { id: 1, isoCode: 'en-us', translated: true },
    { id: 2, isoCode: 'es-es', translated: true }
] as DotLanguage[];

describe('LocalesFeature', () => {
    let spectator: SpectatorService<unknown>;

    let store: unknown;
    let dotLanguagesService: SpyObject<DotLanguagesService>;
    let dotContentletService: SpyObject<DotContentletService>;

    const withTest = () =>
        signalStoreFeature(
            withState(contentInitialState),
            withMethods((store) => ({
                updateContent: () => {
                    patchState(store, { contentlet: { identifier: '123' } as DotCMSContentlet });
                }
            }))
        );

    const createStore = createServiceFactory({
        service: signalStore(withTest(), withLocales()),
        mocks: [DotLanguagesService, DotContentletService, DotHttpErrorManagerService]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotLanguagesService = spectator.inject(DotLanguagesService);
        dotContentletService = spectator.inject(DotContentletService);

        jest.spyOn(dotContentletService, 'getLanguages').mockReturnValue(of(MOCK_LANGUAGES));
        jest.spyOn(dotLanguagesService, 'getDefault').mockReturnValue(of(MOCK_LANGUAGES[1]));
    });

    it('should load locales when a new contentlet is loaded', fakeAsync(() => {
        store.updateContent();
        tick();

        expect(dotContentletService.getLanguages).toHaveBeenCalledWith('123');
        expect(dotLanguagesService.getDefault).toHaveBeenCalledTimes(1);
    }));
});
