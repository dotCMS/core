import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { ActivatedRoute, Router } from '@angular/router';

import { withLayout } from './withLayout';

import { UVEPageParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';

const emptyParams = {} as UVEPageParams;

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: emptyParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withLayout());

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

            expect(store.pageAPIResponse().layout).toEqual(layout);
        });
    });
});
