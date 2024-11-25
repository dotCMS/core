/* eslint-disable @typescript-eslint/no-explicit-any */
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
import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';
import { contentInitialState } from '@dotcms/edit-content/feature/edit-content/store/features/content.feature';
import { withLocales } from '@dotcms/edit-content/feature/edit-content/store/features/locales.feature';

const MOCK_LANGUAGES = [
    { id: 1, isoCode: 'en-us', translated: true },
    { id: 2, isoCode: 'es-es', translated: true }
] as DotLanguage[];

describe('LocalesFeature', () => {
    let spectator: SpectatorService<any>;

    let store: any;
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
    });

    it('should load locales when a new contentlet is loaded', fakeAsync(() => {
        dotContentletService.getLanguages.mockReturnValue(of(MOCK_LANGUAGES));
        dotLanguagesService.getDefault.mockReturnValue(of(MOCK_LANGUAGES[1]));

        expect(store.localesStatus().status).toEqual(ComponentStatus.INIT);

        store.updateContent();
        tick();

        expect(dotContentletService.getLanguages).toHaveBeenCalledWith('123');
        expect(dotLanguagesService.getDefault).toHaveBeenCalledTimes(1);
        expect(store.localesStatus().status).toEqual(ComponentStatus.LOADED);
        expect(store.locales()).toEqual(MOCK_LANGUAGES);
    }));
});
