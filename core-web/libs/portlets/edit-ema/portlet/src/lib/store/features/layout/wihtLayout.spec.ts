import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';

import { withLayout } from './withLayout';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';
import { withPage } from '../page/withPage';

const initialState = createInitialUVEState();

export const uveStoreMock = signalStore(
    withState<UVEState>(initialState),
    withFlags([]),
    withPage(),
    withLayout()
);

describe('withLayout', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of({}),
                    getClientPage: () => of({}),
                    getGraphQLPage: () => of({}),
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        store.setPageAssetResponse({ pageAsset: MOCK_RESPONSE_HEADLESS });
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

            expect(store.pageAsset()?.layout).toEqual(layout);
        });
    });
});
