import { describe } from '@jest/globals';
import { patchState, unprotected } from '@ngrx/signals';
import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotContentDriveService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { testFilterFacadeConformance } from '@dotcms/ui/testing';

import { createAddRelationshipsFilterFacade } from './add-relationships-filter-facade';
import { AddRelationshipsStore } from './add-relationships.store';

import { AddRelationshipsInput } from '../models/add-relationships.models';

/**
 * The shared conformance suite (obligations O1–O9), run against this dialog's facade.
 *
 * Imported from `@dotcms/ui/testing` rather than `@dotcms/ui`: it calls Jest globals, and
 * re-exporting it from the production barrel drags those into every consumer's typecheck.
 */
describe('AddRelationshipsFilterFacade', () => {
    let spectator: SpectatorService<InstanceType<typeof AddRelationshipsStore>>;

    const createService = createServiceFactory({
        service: AddRelationshipsStore,
        providers: [
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(
                    of({
                        list: [],
                        contentCount: 0,
                        folderCount: 0,
                        hasMoreContent: false,
                        hasMoreFolders: false,
                        nextContentCursor: 0,
                        nextFolderCursor: 0
                    })
                )
            }),
            mockProvider(SiteService, { currentSite: { hostname: 'demo.dotcms.com' } })
        ]
    });

    const input: AddRelationshipsInput = {
        contentTypeId: 'target-type',
        selected: [],
        selectionMode: 'multiple',
        contentletContext: { languageId: 1 }
    };

    testFilterFacadeConformance(
        'AddRelationships',
        () => {
            spectator = createService();
            const store = spectator.service;
            store.initialize(input);

            return {
                facade: createAddRelationshipsFilterFacade(store),
                readRawBag: () => store.filters() as Record<string, unknown>,
                readPage: () => store.page().number,
                goToPage2: () => store.setPage(2),
                // What the caller seeded: the contentlet's locale, and nothing else. Clearing
                // returns here rather than to `{}` — an empty set would strand the editor in an
                // unfiltered library.
                expectedDefaults: { languageId: ['1'] },
                // This dialog has no URL, so its bag already holds the vocabulary chips speak and
                // its normalization is the identity. The same assertion passes on Content Drive
                // for the opposite reason — it stores numbers — which is the point of one suite.
                encodedFilter: { key: 'languageId', value: ['2'] },
                writeRaw: (key: string, value: unknown) =>
                    patchState(unprotected(store), {
                        filters: { ...store.filters(), [key]: value as string | string[] }
                    }),
                /**
                 * O8. The relationship's target content type is a caller restriction, not a
                 * filter: it lives on the store's own `contentTypeId` and there is no code path
                 * from the facade to it. That is what makes "the editor cannot widen past the
                 * type this field accepts" structural rather than a convention.
                 */
                restrictedKeys: ['contentTypes', 'contentTypeId']
            };
        },
        { normalizes: false }
    );
});
