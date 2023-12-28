import { SpectatorService } from '@ngneat/spectator';
import { createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService
} from '@dotcms/data-access';

import {
    DotPaletteStore,
    EditEmaPaletteStoreStatus,
    PALETTE_PAGINATOR_ITEMS_PER_PAGE
} from './edit-ema-palette.store';

describe('EditEmaPaletteStore', () => {
    let spectator: SpectatorService<DotPaletteStore>;
    const createService = createServiceFactory({
        service: DotPaletteStore,
        providers: [
            {
                provide: DotPropertiesService,
                useValue: {
                    getKeyAsList: () => of([])
                }
            }
        ],
        mocks: [DotContentTypeService, DotESContentService]
    });

    beforeEach(() => {
        spectator = createService();
    });

    describe('updaters', () => {
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

        it('should load allowed content types', (done) => {
            spectator.service.loadAllowedContentTypes({ containers: {} });
            spectator.service.vm$.subscribe((state) => {
                expect(state.allowedTypes).toEqual([]);
                done();
            });
        });
    });
});
