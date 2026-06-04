import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withFeature, withState } from '@ngrx/signals';
import { of, Subject, throwError } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotPageLayoutService,
    DotPropertiesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotLanguage } from '@dotcms/dotcms-models';
import { DotPageAssetLayoutRow, UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';

import { withPageApi } from './withPageApi';

import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS, ACTION_PAYLOAD_MOCK } from '../../../shared/mocks';
import { IframeAccessMode, UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';
import { withPage } from '../page/withPage';

const pageParamsBase = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: 'dot:persona',
    mode: UVE_MODE.EDIT
};

// Deliberately omits a `url` variable: exercises the legacy-client branch where
// pageLoad assumes the stored request belongs to the page currently loaded
// (falls back to comparing against the previous pageParams.url).
const graphqlRequestWithoutUrl = {
    query: '{ page { url } }',
    variables: { depth: '1', language_id: '1' }
};

function buildTestStore() {
    return signalStore(
        { protectedState: false },
        withState<UVEState>(createInitialUVEState({ pageParams: pageParamsBase })),
        withFlags([]),
        withPage(),
        withFeature((store) =>
            withPageApi({
                resetClientConfiguration: () => store.resetClientConfiguration(),
                markPageLoading: () => store.markPageLoading(),
                resetRequestMetadata: () => store.resetRequestMetadata(),
                requestMetadata: () => store.requestMetadata(),
                $requestWithParams: store.$requestWithParams,
                setPageAsset: (payload) => store.setPageAsset(payload),
                rollbackPageAssetResponse: () => store.rollbackPageAssetResponse(),
                addHistory: (response) => store.addToHistory(response),
                resetHistoryToCurrent: () => store.resetHistoryToCurrent(),
                pageAsset: () => store.pageAsset()
            })
        )
    );
}

describe('withPageApi', () => {
    let spectator: SpectatorService<InstanceType<ReturnType<typeof buildTestStore>>>;
    let store: InstanceType<ReturnType<typeof buildTestStore>>;

    const getSpy = jest.fn((_params?: unknown) => of(MOCK_RESPONSE_HEADLESS));
    const getGraphQLPageSpy = jest.fn((_params?: unknown) =>
        of({
            pageAsset: MOCK_RESPONSE_HEADLESS,
            content: { source: 'graphql' }
        })
    );

    const createService = createServiceFactory({
        service: buildTestStore(),
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotExperimentsService, {
                getById: jest.fn().mockReturnValue(of(null))
            }),
            mockProvider(DotLanguagesService, {
                getLanguagesUsedPage: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotPageLayoutService, {
                save: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(UveIframeMessengerService, {
                sendPageData: jest.fn()
            }),
            {
                provide: WINDOW,
                useValue: {
                    location: {
                        origin: 'https://editor.dotcms.com'
                    }
                }
            },
            mockProvider(DotWorkflowActionsFireService, {
                saveContentlet: jest.fn().mockReturnValue(of({}))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: getSpy,
                    getGraphQLPage: getGraphQLPageSpy,
                    save: jest.fn().mockReturnValue(of({})),
                    saveStyleProperties: jest.fn().mockReturnValue(of({}))
                }
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        store = spectator.service;
        spectator.flushEffects();
    });

    describe('pageLoad – fetch vs GraphQL', () => {
        it('should use the injected window origin when computing iframe access mode in pageUpdateParams', () => {
            store.pageUpdateParams({
                clientHost: 'https://editor.dotcms.com/headless'
            });

            expect(store.iframeAccessMode()).toBe(IframeAccessMode.LOCAL);
        });

        it('should use regular get(pageParams) when requestMetadata is null', () => {
            expect(store.requestMetadata()).toBeNull();

            store.pageLoad({ language_id: '1' });
            spectator.flushEffects();

            expect(getSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    ...pageParamsBase,
                    language_id: '1'
                })
            );
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should use getGraphQLPage when requestMetadata is set', () => {
            store.setCustomClient(graphqlRequestWithoutUrl);
            expect(store.requestMetadata()).toEqual(graphqlRequestWithoutUrl);

            store.pageLoad({ language_id: '2' });
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalled();
            expect(getSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should pass merged params to getGraphQLPage via $requestWithParams', () => {
            store.setCustomClient({ query: 'query', variables: { depth: '1' } });
            store.pageUpdateParams({ language_id: '3' });

            store.pageLoad({});
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: 'query',
                    variables: expect.objectContaining({
                        depth: '1',
                        // $requestWithParams merges pageParams into variables (friendly keys)
                        languageId: '3',
                        url: 'test-url'
                    })
                })
            );
        });

        it('should set page asset with content when GraphQL response includes content', () => {
            store.setCustomClient(graphqlRequestWithoutUrl);

            store.pageLoad({});
            spectator.flushEffects();

            const response = store.pageAssetResponse();
            expect(response?.pageAsset).toEqual(MOCK_RESPONSE_HEADLESS);
            expect(response?.content).toEqual({ source: 'graphql' });
        });

        it('should drop the stored client request and use get() when navigating to a different page', () => {
            // Page One's CLIENT_READY installed its GraphQL request
            store.setCustomClient(graphqlRequestWithoutUrl);
            expect(store.requestMetadata()).toEqual(graphqlRequestWithoutUrl);

            // The stored request belongs to the current page ('test-url'):
            // navigating to another page must NOT reuse its query/variables
            store.pageLoad({ url: 'another-page' });
            spectator.flushEffects();

            expect(store.requestMetadata()).toBeNull();
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(getSpy).toHaveBeenCalledWith(expect.objectContaining({ url: 'another-page' }));
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should drop a stored request explicitly captured for another page (NAVIGATION_UPDATE before CLIENT_READY)', () => {
            // The stored request declares the page it belongs to via its own
            // url variable (current page, '/test-url'). If NAVIGATION_UPDATE
            // arrives before the new page's CLIENT_READY, pageLoad must not
            // reuse it — it falls back to the standard Page API (first-load flow)
            store.setCustomClient({
                query: 'query',
                variables: { url: '/test-url', depth: '1' }
            });

            store.pageLoad({ url: 'another-page' });
            spectator.flushEffects();

            expect(store.requestMetadata()).toBeNull();
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(getSpy).toHaveBeenCalledWith(expect.objectContaining({ url: 'another-page' }));
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should keep the stored client request when it was captured for the target page', () => {
            // Client-side navigation: the new page's CLIENT_READY already
            // installed its own request (variables.url points to the target)
            store.setCustomClient({
                query: 'query',
                variables: { url: '/another-page', depth: '1' }
            });

            store.pageLoad({ url: 'another-page' });
            spectator.flushEffects();

            expect(store.requestMetadata()).not.toBeNull();
            expect(getGraphQLPageSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    variables: expect.objectContaining({ url: 'another-page' })
                })
            );
            expect(getSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should keep the stored client request when navigating to the same page (other params)', () => {
            store.setCustomClient(graphqlRequestWithoutUrl);

            // Same pathname, leading-slash variant — still the same page
            store.pageLoad({ url: '/test-url', language_id: '2' });
            spectator.flushEffects();

            expect(store.requestMetadata()).toEqual(graphqlRequestWithoutUrl);
            expect(getGraphQLPageSpy).toHaveBeenCalled();
            expect(getSpy).not.toHaveBeenCalled();
        });

        it('should drop the stored client request when there are no previous pageParams', () => {
            patchState(store, { pageParams: null });
            store.setCustomClient(graphqlRequestWithoutUrl);

            // Fresh load (e.g. shell re-created after leaving the portlet):
            // stale metadata from a previous visit must not leak into the new page
            store.pageLoad({ ...pageParamsBase, url: 'fresh-page' });
            spectator.flushEffects();

            expect(store.requestMetadata()).toBeNull();
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(getSpy).toHaveBeenCalled();
        });

        it('should reset editorSelected when loading a new page', () => {
            patchState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: ACTION_PAYLOAD_MOCK
                }
            });
            expect(store.editorSelected()).not.toBeNull();

            store.pageLoad({ language_id: '1' });

            expect(store.editorSelected()).toBeNull();
        });
    });

    describe('updateRows', () => {
        const MOCK_ROWS: DotPageAssetLayoutRow[] = [
            {
                identifier: 1,
                styleClass: 'row-class',
                metadata: { name: 'Hero Section' },
                columns: [
                    {
                        preview: false,
                        containers: [{ identifier: 'container-1', uuid: '1', historyUUIDs: [] }],
                        widthPercent: 100,
                        width: 12,
                        leftOffset: 1,
                        left: 0,
                        styleClass: 'col-class',
                        metadata: { name: 'Main Column' }
                    }
                ]
            }
        ];

        it('should include column metadata in the save payload', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            const layoutService = spectator.inject(DotPageLayoutService);

            store.updateRows(MOCK_ROWS);
            spectator.flushEffects();

            expect(layoutService.save).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    layout: expect.objectContaining({
                        body: expect.objectContaining({
                            rows: expect.arrayContaining([
                                expect.objectContaining({
                                    columns: expect.arrayContaining([
                                        expect.objectContaining({
                                            metadata: { name: 'Main Column' }
                                        })
                                    ])
                                })
                            ])
                        })
                    })
                })
            );
        });

        it('should handle columns without metadata', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            const layoutService = spectator.inject(DotPageLayoutService);

            const rowsWithoutMetadata: DotPageAssetLayoutRow[] = [
                {
                    identifier: 1,
                    columns: [
                        {
                            preview: false,
                            containers: [],
                            widthPercent: 100,
                            width: 12,
                            leftOffset: 1,
                            left: 0
                        }
                    ]
                }
            ];

            store.updateRows(rowsWithoutMetadata);
            spectator.flushEffects();

            expect(layoutService.save).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    layout: expect.objectContaining({
                        body: expect.objectContaining({
                            rows: expect.arrayContaining([
                                expect.objectContaining({
                                    columns: expect.arrayContaining([
                                        expect.objectContaining({
                                            metadata: undefined
                                        })
                                    ])
                                })
                            ])
                        })
                    })
                })
            );
        });
    });

    describe('saveQuickEditFields', () => {
        it('should include DEFAULT variantName when pageParams has no variantName', () => {
            const saveContentletSpy = jest.spyOn(
                spectator.inject(DotWorkflowActionsFireService),
                'saveContentlet'
            );

            store.saveQuickEditFields({ inode: 'test-inode', title: 'New Title' });

            expect(saveContentletSpy).toHaveBeenCalledWith(
                expect.objectContaining({ variantName: DEFAULT_VARIANT_ID })
            );
        });

        it('should include the active variantName from pageParams when set', () => {
            const saveContentletSpy = jest.spyOn(
                spectator.inject(DotWorkflowActionsFireService),
                'saveContentlet'
            );
            store.pageUpdateParams({ variantName: 'my-experiment-variant' });

            store.saveQuickEditFields({ inode: 'test-inode', title: 'New Title' });

            expect(saveContentletSpy).toHaveBeenCalledWith(
                expect.objectContaining({ variantName: 'my-experiment-variant' })
            );
        });
    });

    describe('pageReload – fetch vs GraphQL', () => {
        it('should call get when reloading without GraphQL metadata', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            jest.clearAllMocks();

            store.pageReload();
            spectator.flushEffects();

            expect(getSpy).toHaveBeenCalled();
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should call getGraphQLPage when reloading with GraphQL metadata', () => {
            store.setCustomClient(graphqlRequestWithoutUrl);
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            jest.clearAllMocks();

            store.pageReload();
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalled();
            expect(getSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should have pageLanguages already updated when setPageAsset is called during reload', () => {
            // Regression test for #35647. The pre-fix code called setPageAsset BEFORE
            // getLanguagesUsedPage responded, so pageTranslateProps (which reacts to
            // pageAsset but reads pageLanguages via untracked()) saw stale data.
            // We verify atomicity by holding getLanguagesUsedPage open with a Subject:
            // the page asset must NOT be updated until the Subject emits, proving both
            // writes happen in the same tap (after languages resolve).
            const freshPage = {
                ...MOCK_RESPONSE_HEADLESS,
                page: { ...MOCK_RESPONSE_HEADLESS.page, title: 'Reloaded' }
            };
            const languagesSubject = new Subject<DotLanguage[]>();

            getSpy.mockReturnValueOnce(of(freshPage));
            jest.spyOn(
                spectator.inject(DotLanguagesService),
                'getLanguagesUsedPage'
            ).mockReturnValue(languagesSubject);

            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });

            store.pageReload();
            spectator.flushEffects();

            // Languages haven't resolved yet — page asset must still show the original title.
            // Pre-fix code would have already swapped to 'Reloaded' here.
            expect(store.pageAssetResponse()?.pageAsset.page.title).toBe('Test Page');

            languagesSubject.next([
                { id: 1, language: 'English', languageCode: 'en', translated: true }
            ]);
            languagesSubject.complete();
            spectator.flushEffects();

            // After languages resolve, both pageAsset and pageLanguages are updated atomically.
            expect(store.pageAssetResponse()?.pageAsset.page.title).toBe('Reloaded');
            expect(store.pageLanguages()[0].translated).toBe(true);
        });

        it('should apply the page asset and set status to LOADED when getLanguagesUsedPage fails', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });

            jest.spyOn(
                spectator.inject(DotLanguagesService),
                'getLanguagesUsedPage'
            ).mockReturnValue(throwError(() => ({ status: 500 })));

            store.pageReload();
            spectator.flushEffects();

            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
            expect(store.pageAssetResponse()?.pageAsset).toEqual(MOCK_RESPONSE_HEADLESS);
        });
    });
});
