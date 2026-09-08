import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import {
    AddToBundleService,
    DotBulkRefreshService,
    DotContentDriveService,
    DotCurrentUserService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotPropertiesService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { testFilterFacadeConformance } from '@dotcms/ui/testing';
import { mockLocales } from '@dotcms/utils-testing';

import { createContentDriveFilterFacade } from './content-drive-filter-facade';
import { DotContentDriveStore } from './dot-content-drive.store';

import { MAP_BASE_TYPES_TO_NUMBERS } from '../shared/constants';

/**
 * Content Drive's half of the shared conformance suite.
 *
 * The point of running the *same* suite here and on the AssetPicker is that these two surfaces
 * store filters differently — Content Drive encodes base types as the numeric keys its URL needs,
 * the picker keeps names — and one shared suite is what stops the two encodings drifting apart.
 *
 * `mockLocales[0]` is the default language (`id: 1`), which the store resolves on init and seeds
 * into the `languageId` filter, so that plus the shared-assets toggle is what "cleared" looks like.
 */
describe('Content Drive filter facade', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, { snapshot: { queryParams: {} } }),
            mockProvider(GlobalStore, { siteDetails: jest.fn().mockReturnValue(undefined) }),
            mockProvider(DotContentDriveService),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ roles: [] }))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService, { getEnvironments: jest.fn(() => of([])) }),
            mockProvider(DotBulkRefreshService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            // Answers synchronously so the default-language seed is in place before the suite runs.
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(mockLocales))
            }),
            provideHttpClient()
        ]
    });

    testFilterFacadeConformance(
        'Content Drive',
        () => {
            spectator = createService();
            const store = spectator.service;
            // The default language is seeded by the store's init effect, and every default the
            // suite asserts depends on it having landed.
            spectator.flushEffects();

            return {
                facade: createContentDriveFilterFacade(store),
                readRawBag: () => store.filters() as Record<string, unknown>,
                readPage: () => store.pagination().page,
                goToPage2: () => store.setPagination({ ...store.pagination(), page: 2 }),
                // What `withFilterDefaults` produces: the environment language, plus the
                // shared-assets toggle written out explicitly so the URL always states it.
                expectedDefaults: {
                    languageId: ['1'],
                    sharedAssets: 'true'
                },
                // Names in, names out — but stored as the numeric keys the URL round-trips. This is
                // the assertion the picker also runs, and passing it here proves the translation is
                // lossless in both directions.
                encodedFilter: {
                    key: 'baseType',
                    value: [DotCMSBaseTypesContentTypes.DOTASSET]
                },
                writeRaw: (key: string, value: unknown) =>
                    patchState(unprotected(store), {
                        filters: { ...store.filters(), [key]: value as string | string[] }
                    }),
                // A base-type key no map entry covers. It must be dropped on the way out, not
                // handed to a chip that would render it as a selected option nobody can clear.
                unmappableRawValue: ['9999'],
                // Content Drive imposes none — it is the unrestricted surface, which is exactly why
                // running O8 here is still worth it: it proves the facade invents no restriction.
                restrictedKeys: []
            };
        },
        // Base-type names cross a real boundary here, so O7's "drop the unmappable" applies.
        { normalizes: true }
    );

    it('should store base types as the numeric keys the URL round-trips', () => {
        spectator = createService();
        const store = spectator.service;
        spectator.flushEffects();
        const facade = createContentDriveFilterFacade(store);

        facade.patchFilters({ baseType: [DotCMSBaseTypesContentTypes.DOTASSET] });

        // The whole reason the facade exists: chips never see this encoding.
        expect(store.getFilterValue('baseType')).toEqual([
            MAP_BASE_TYPES_TO_NUMBERS[DotCMSBaseTypesContentTypes.DOTASSET]
        ]);
    });
});
