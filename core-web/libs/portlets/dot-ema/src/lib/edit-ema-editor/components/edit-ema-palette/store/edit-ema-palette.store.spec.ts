import { SpectatorService } from '@ngneat/spectator';
import { createServiceFactory } from '@ngneat/spectator/jest';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';

import { DotPaletteStore } from './edit-ema-palette.store';

import { PALETTE_PAGINATOR_ITEMS_PER_PAGE } from '../shared/edit-ema-palette.const';
import { EditEmaPaletteStoreStatus, PALETTE_TYPES } from '../shared/edit-ema-palette.enums';

describe('EditEmaPaletteStore', () => {
    let spectator: SpectatorService<DotPaletteStore>;
    const createService = createServiceFactory({
        service: DotPaletteStore,
        mocks: [DotContentTypeService, DotESContentService]
    });

    beforeEach(() => {
        spectator = createService();
    });

    describe('updaters', () => {
        it('should change view', (done) => {
            spectator.service.changeView(PALETTE_TYPES.CONTENTLET);
            spectator.service.vm$.subscribe((state) => {
                expect(state.currentPaletteType).toEqual(PALETTE_TYPES.CONTENTLET);
                done();
            });
        });

        it('should set loading', (done) => {
            spectator.service.setStatus(EditEmaPaletteStoreStatus.LOADING);
            spectator.service.vm$.subscribe((state) => {
                expect(state.status).toEqual(EditEmaPaletteStoreStatus.LOADING);
                done();
            });
        });

        it('should reset contentlets', (done) => {
            spectator.service.resetContentlets();
            spectator.service.vm$.subscribe((state) => {
                expect(state.contentlets).toEqual({
                    items: [],
                    filter: { query: '', contentTypeVarName: '' },
                    totalRecords: 0,
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                });
                done();
            });
        });
    });

    describe('effects', () => {
        it('should load content types', (done) => {
            spectator.service.loadContentTypes({
                filter: '',
                allowedContent: []
            });
            spectator.service.vm$.subscribe((state) => {
                expect(state.contenttypes.items).toEqual([]);
                done();
            });
        });

        it('should load contentlets', (done) => {
            spectator.service.loadContentlets({ filter: '', contenttypeName: '', languageId: '1' });
            spectator.service.vm$.subscribe((state) => {
                expect(state.contentlets.items).toEqual([]);
                done();
            });
        });
    });
});
