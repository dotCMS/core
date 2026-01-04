import { describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';

import { ActivatedRoute, Router } from '@angular/router';

import { UVE_MODE } from '@dotcms/types';

import { withClient } from './withClient';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { Orientation, UVEState } from '../../models';

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: emptyParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    isClientReady: false,
    // Phase 3: Nested editor state
    editor: {
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        panels: {
            palette: { open: true },
            rightSidebar: { open: false }
        },
        ogTags: null,
        styleSchemas: []
    },
    // Phase 3: Nested toolbar state
    toolbar: {
        device: null,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withClient()
);

describe('UVEStore', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [mockProvider(Router), mockProvider(ActivatedRoute)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should have initial state', () => {
        expect(store.isClientReady()).toBeFalsy();
        expect(store.graphql()).toEqual(null);
        expect(store.graphqlResponse()).toEqual(null);
        expect(store.isClientReady()).toBe(false);
        expect(store.legacyGraphqlResponse()).toBe(false);
    });

    describe('withMethods', () => {
        it('should set the client ready status', () => {
            store.setIsClientReady(true);

            expect(store.isClientReady()).toBe(true);
        });

        describe('setClientConfiguration', () => {
            it('should set the client configuration', () => {
                const graphql = {
                    query: 'test',
                    variables: {
                        depth: '1'
                    }
                };

                store.setCustomGraphQL(graphql, true);

                expect(store.graphql()).toEqual(graphql);
            });
        });

        it('should reset the client configuration', () => {
            const graphql = {
                query: 'test',
                variables: null
            };

            store.setCustomGraphQL(graphql, true);
            store.resetClientConfiguration();

            expect(store.graphql()).toEqual(null);
        });
    });

    describe('$graphqlWithParams', () => {
        it('should return null when graphql is null', () => {
            expect(store.$graphqlWithParams()).toBeNull();
        });

        it('should return null when graphql is not set', () => {
            store.resetClientConfiguration();
            expect(store.$graphqlWithParams()).toBeNull();
        });

        it('should merge graphql variables with page params', () => {
            const graphql = {
                query: 'test query',
                variables: {
                    depth: '1'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/test-url',
                mode: UVE_MODE.EDIT,
                language_id: '1',
                variantName: 'DEFAULT',
                [PERSONA_KEY]: 'persona-id-123'
            };

            patchState(store, { pageParams });
            store.setCustomGraphQL(graphql, false);

            const result = store.$graphqlWithParams();

            expect(result).toEqual({
                query: 'test query',
                variables: {
                    depth: '1',
                    url: '/test-url',
                    mode: UVE_MODE.EDIT,
                    languageId: '1',
                    variantName: 'DEFAULT',
                    personaId: 'persona-id-123'
                }
            });
        });

        it('should preserve existing graphql variables when merging with page params', () => {
            const graphql = {
                query: 'test query',
                variables: {
                    depth: '2',
                    customVar: 'custom-value'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/another-url',
                mode: UVE_MODE.PREVIEW,
                language_id: '2',
                variantName: 'VARIANT_A',
                [PERSONA_KEY]: 'persona-id-456'
            };

            patchState(store, { pageParams });
            store.setCustomGraphQL(graphql, false);

            const result = store.$graphqlWithParams();

            expect(result).toEqual({
                query: 'test query',
                variables: {
                    depth: '2',
                    customVar: 'custom-value',
                    url: '/another-url',
                    mode: UVE_MODE.PREVIEW,
                    languageId: '2',
                    variantName: 'VARIANT_A',
                    personaId: 'persona-id-456'
                }
            });
        });

        it('should handle missing optional page params', () => {
            const graphql = {
                query: 'test query',
                variables: {
                    depth: '1'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/test-url',
                language_id: '1',
                [PERSONA_KEY]: 'persona-id-123'
                // mode and variantName are missing
            };

            patchState(store, { pageParams });
            store.setCustomGraphQL(graphql, false);

            const result = store.$graphqlWithParams();

            expect(result).toEqual({
                query: 'test query',
                variables: {
                    depth: '1',
                    url: '/test-url',
                    mode: undefined,
                    languageId: '1',
                    variantName: undefined,
                    personaId: 'persona-id-123'
                }
            });
        });
    });
});
