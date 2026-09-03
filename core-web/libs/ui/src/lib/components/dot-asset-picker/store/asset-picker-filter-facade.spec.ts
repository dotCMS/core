import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotContentDriveService, DotFolderService, DotSiteService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveSearchResponse,
    DotSite
} from '@dotcms/dotcms-models';

import { createAssetPickerFilterFacade } from './asset-picker-filter-facade';
import { DotAssetPickerStore } from './dot-asset-picker.store';
import { DotAssetPickerConfig } from './models';

import { testFilterFacadeConformance } from '../../dot-filter-bar/testing/filter-facade.conformance';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const EMPTY_RESPONSE: DotContentDriveSearchResponse = {
    folderCount: 0,
    contentCount: 0,
    list: [],
    hasMoreContent: false,
    hasMoreFolders: false,
    nextContentCursor: 0,
    nextFolderCursor: 0
};

const ASSET_BASE_TYPES = [
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.FILEASSET
];

/**
 * The Image field's configuration: a type boundary that is also pre-selected, a locale, and a
 * silent mimetype restriction.
 *
 * Chosen over the File field's on purpose — it is the entry point with the most to get wrong. It
 * seeds two filters (so O5/O6 have real defaults to land on) *and* carries two caller restrictions
 * (so O8 has something to keep hidden).
 */
const CONFIG: DotAssetPickerConfig = {
    site: SITE,
    languageId: '1',
    allowedBaseTypes: ASSET_BASE_TYPES,
    baseTypes: ASSET_BASE_TYPES,
    mimeTypes: ['image/*']
};

describe('AssetPicker filter facade', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAssetPickerStore>>;

    const createService = createServiceFactory({
        service: DotAssetPickerStore,
        providers: [
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(EMPTY_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                searchFolders: jest.fn().mockReturnValue(of({ folders: [], pagination: {} }))
            }),
            mockProvider(DotSiteService, {
                getSites: jest.fn().mockReturnValue(of({ sites: [], pagination: {} }))
            })
        ]
    });

    testFilterFacadeConformance(
        'AssetPicker',
        () => {
            spectator = createService();
            const store = spectator.service;
            store.initPicker(CONFIG);

            return {
                facade: createAssetPickerFilterFacade(store),
                readRawBag: () => store.filters() as Record<string, unknown>,
                readPage: () => store.pagination().page,
                goToPage2: () => store.setPagination({ ...store.pagination(), page: 2 }),
                // What the caller seeded. `sharedAssets` is deliberately absent: the picker has no
                // URL, so there is nothing to spell the applied state out for, and an absent key
                // already means on.
                expectedDefaults: {
                    languageId: ['1'],
                    baseType: ASSET_BASE_TYPES
                },
                // Base types by NAME, stored by name. The same assertion passes on Content Drive
                // for the opposite reason — it stores numbers — which is the point of one suite.
                encodedFilter: {
                    key: 'baseType',
                    value: [DotCMSBaseTypesContentTypes.DOTASSET]
                },
                writeRaw: (key: string, value: unknown) =>
                    patchState(unprotected(store), {
                        filters: { ...store.filters(), [key]: value as string | string[] }
                    }),
                // Both live on `config`, never in the filter bag. An Image field that could return
                // a PDF is broken, so O8 proves the facade cannot even see them.
                restrictedKeys: ['mimeTypes', 'allowedBaseTypes', 'live']
            };
        },
        // Normalization is the identity here — the picker stores exactly the vocabulary chips
        // speak, so it has no unmappable state to drop.
        { normalizes: false }
    );
});
