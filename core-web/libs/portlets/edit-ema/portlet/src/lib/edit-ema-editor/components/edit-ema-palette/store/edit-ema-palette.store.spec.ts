import { describe, it, expect } from '@jest/globals';
import { SpectatorService } from '@ngneat/spectator';
import { createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService
} from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import {
    DotPaletteStore,
    EditEmaPaletteStoreStatus,
    PALETTE_PAGINATOR_ITEMS_PER_PAGE,
    PALETTE_TYPES
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
            },
            {
                provide: DotESContentService,
                useValue: {
                    get: () =>
                        of({
                            jsonObjectView: { contentlets: [] },
                            resultsSize: 0
                        })
                }
            },
            {
                provide: DotContentTypeService,
                useValue: {
                    filterContentTypes: () =>
                        of([
                            {
                                baseType: 'someContent',
                                id: 'contentTypeTest1',
                                name: 'contentTypeTest1'
                            },
                            {
                                baseType: 'WIDGET',
                                id: 'contentTypeTest2',
                                name: 'contentTypeTest2'
                            }
                        ]),
                    getContentTypes: () =>
                        of([
                            {
                                baseType: 'WIDGET',
                                id: 'widgetTest1',
                                name: 'widgetTest1'
                            }
                        ])
                }
            }
        ]
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
            const patchStateSpy = jest.spyOn(spectator.service, 'patchState');
            const filterContentTypesSpy = jest.spyOn(
                spectator.inject(DotContentTypeService),
                'filterContentTypes'
            );
            const getContentTypesSpy = jest.spyOn(
                spectator.inject(DotContentTypeService),
                'getContentTypes'
            );
            spectator.service.loadContentTypes({
                filter: '',
                allowedContent: ['contentTypeTest1']
            });
            const expected = [
                {
                    baseType: 'someContent',
                    id: 'contentTypeTest1',
                    name: 'contentTypeTest1'
                },
                {
                    baseType: 'WIDGET',
                    id: 'widgetTest1',
                    name: 'widgetTest1'
                }
            ];
            spectator.service.vm$.subscribe((state) => {
                expect(state.contenttypes.items).toEqual(expected as DotCMSContentType[]);
                expect(patchStateSpy).toHaveBeenCalled();
                expect(filterContentTypesSpy).toHaveBeenCalledWith('', 'contentTypeTest1');
                expect(getContentTypesSpy).toHaveBeenCalledWith({
                    filter: '',
                    page: 40,
                    type: 'WIDGET'
                });
                done();
            });
        });

        it('should load contentlets', (done) => {
            const contentServiceSpy = jest.spyOn(spectator.inject(DotESContentService), 'get');
            const patchStateSpy = jest.spyOn(spectator.service, 'patchState');
            spectator.service.loadContentlets({
                filter: '',
                contenttypeName: 'TestContentType1',
                languageId: '1'
            });
            spectator.service.vm$.subscribe((state) => {
                expect(contentServiceSpy).toHaveBeenCalledWith({
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE,
                    lang: '1',
                    filter: '',
                    offset: '0',
                    query: '+contentType:TestContentType1 +deleted:false +variant:DEFAULT'
                });
                expect(patchStateSpy).toHaveBeenCalled();
                expect(state).toEqual({
                    contentlets: {
                        filter: {
                            query: '',
                            contentTypeVarName: 'TestContentType1'
                        },
                        items: [],
                        totalRecords: 0,
                        itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                    },
                    contenttypes: {
                        filter: '',
                        items: []
                    },
                    status: EditEmaPaletteStoreStatus.LOADED,
                    currentContentType: 'TestContentType1',
                    currentPaletteType: PALETTE_TYPES.CONTENTLET,
                    allowedTypes: []
                });
                done();
            });
        });

        it('should load contentlets with variant', (done) => {
            const contentServiceSpy = jest.spyOn(spectator.inject(DotESContentService), 'get');
            const patchStateSpy = jest.spyOn(spectator.service, 'patchState');

            contentServiceSpy.mockClear(); // clear the spy because it had the register of the last call

            spectator.service.loadContentlets({
                filter: '',
                contenttypeName: 'TestContentType1',
                languageId: '1',
                variantId: 'cool-variant'
            });

            spectator.service.vm$.subscribe((state) => {
                expect(contentServiceSpy).toHaveBeenCalledWith({
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE,
                    lang: '1',
                    filter: '',
                    offset: '0',
                    query: '+contentType:TestContentType1 +deleted:false +variant:(DEFAULT OR cool-variant)'
                });
                expect(patchStateSpy).toHaveBeenCalled();
                expect(state).toEqual({
                    contentlets: {
                        filter: {
                            query: '',
                            contentTypeVarName: 'TestContentType1'
                        },
                        items: [],
                        totalRecords: 0,
                        itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                    },
                    contenttypes: {
                        filter: '',
                        items: []
                    },
                    status: EditEmaPaletteStoreStatus.LOADED,
                    currentContentType: 'TestContentType1',
                    currentPaletteType: PALETTE_TYPES.CONTENTLET,
                    allowedTypes: []
                });
                done();
            });
        });

        it('should load allowed content types', (done) => {
            const setAllowedContentTypesSpy = jest.spyOn(spectator.service, 'setAllowedTypes');
            const loadContentTypesSpy = jest.spyOn(spectator.service, 'loadContentTypes');
            spectator.service.loadAllowedContentTypes({ containers: {} });
            spectator.service.vm$.subscribe((state) => {
                expect(state.allowedTypes).toEqual([]);
                expect(setAllowedContentTypesSpy).toHaveBeenCalled();
                expect(loadContentTypesSpy).toHaveBeenCalled();
                done();
            });
        });
    });
});
