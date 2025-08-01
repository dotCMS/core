import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

// Mock the getUVEState function
jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE, PRODUCTION_MODE } from '@dotcms/uve/internal';

import { DotCMSStore, EMPTY_DOTCMS_PAGE_STORE } from './dotcms.store';

import { DotCMSPageStore } from '../models';
import { PageResponseMock } from '../utils/testing.utils';
describe('DotCMSStore', () => {
    let spectator: SpectatorService<DotCMSStore>;
    let service: DotCMSStore;

    const createService = createServiceFactory(DotCMSStore);

    beforeEach(() => {
        TestBed.configureTestingModule({});
        spectator = createService();
        service = spectator.service;
        jest.clearAllMocks();
    });

    it('should return the empty store', () => {
        expect(service.store).toEqual(EMPTY_DOTCMS_PAGE_STORE);
    });

    it('should set and get store', () => {
        const mockStore: DotCMSPageStore = {
            page: PageResponseMock,
            components: {},
            mode: PRODUCTION_MODE
        };

        service.setStore(mockStore);

        expect(service.store).toEqual(mockStore);
    });

    describe('$isDevMode', () => {
        it('should return true when UVE mode is EDIT', () => {
            (getUVEState as jest.Mock).mockReturnValue({ mode: UVE_MODE.EDIT });

            expect(service.$isDevMode()).toBe(true);
        });

        it('should return false when UVE mode is not EDIT', () => {
            (getUVEState as jest.Mock).mockReturnValue({ mode: UVE_MODE.PREVIEW });

            expect(service.$isDevMode()).toBe(false);
        });

        it('should use store mode when UVE state is not available', () => {
            (getUVEState as jest.Mock).mockReturnValue(undefined);

            service.setStore({
                page: {} as DotCMSPageAsset,
                components: {},
                mode: DEVELOPMENT_MODE
            });

            expect(service.$isDevMode()).toBe(true);
        });

        it('should return false when store mode is PRODUCTION and UVE state is not available', () => {
            (getUVEState as jest.Mock).mockReturnValue(undefined);

            service.setStore({
                page: {} as DotCMSPageAsset,
                components: {},
                mode: PRODUCTION_MODE
            });

            expect(service.$isDevMode()).toBe(false);
        });
    });
});
