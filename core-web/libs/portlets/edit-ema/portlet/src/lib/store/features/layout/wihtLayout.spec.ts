import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withFeature, withState } from '@ngrx/signals';

import { ActivatedRoute, Router } from '@angular/router';

import { withClient } from '../client/withClient';
import { withLayout } from './withLayout';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { Orientation, PageType, UVEState } from '../../models';

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    // Normalized page response properties
    page: MOCK_RESPONSE_HEADLESS.page,
    site: MOCK_RESPONSE_HEADLESS.site,
    template: MOCK_RESPONSE_HEADLESS.template,
    containers: MOCK_RESPONSE_HEADLESS.containers,
    viewAs: MOCK_RESPONSE_HEADLESS.viewAs,
    vanityUrl: MOCK_RESPONSE_HEADLESS.vanityUrl,
    urlContentMap: MOCK_RESPONSE_HEADLESS.urlContentMap,
    numberContents: MOCK_RESPONSE_HEADLESS.numberContents,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: emptyParams,
    status: UVE_STATUS.LOADING,
    pageType: PageType.TRADITIONAL,
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
    // Phase 3: Nested view state
    view: {
        device: null,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        viewParams: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const uveStoreMock = signalStore(
    withState<UVEState>(initialState),
    withFeature(() => withClient()),
    withLayout()
);

describe('withLayout', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [mockProvider(Router), mockProvider(ActivatedRoute)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        store.setGraphqlResponse({ pageAsset: MOCK_RESPONSE_HEADLESS });
    });

    describe('withComputed', () => {
        describe('$layoutProps', () => {
            it('should return the layout props', () => {
                expect(store.$layoutProps()).toEqual({
                    containersMap: mapContainerStructureToDotContainerMap(
                        MOCK_RESPONSE_HEADLESS.containers
                    ),
                    layout: MOCK_RESPONSE_HEADLESS.layout,
                    template: {
                        identifier: MOCK_RESPONSE_HEADLESS.template.identifier,
                        themeId: MOCK_RESPONSE_HEADLESS.template.theme,
                        anonymous: false
                    },
                    pageId: MOCK_RESPONSE_HEADLESS.page.identifier
                });
            });
        });
    });

    describe('withMethods', () => {
        it('should update the layout', () => {
            const layout = {
                ...MOCK_RESPONSE_HEADLESS.layout,
                title: 'New layout'
            };

            store.updateLayout(layout);

            expect(store.layout()).toEqual(layout);
        });
    });
});
