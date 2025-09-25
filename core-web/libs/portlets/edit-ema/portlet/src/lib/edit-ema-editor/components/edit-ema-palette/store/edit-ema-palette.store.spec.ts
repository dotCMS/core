import { describe, it, expect } from '@jest/globals';
import { SpectatorService } from '@ngneat/spectator';
import { createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService
} from '@dotcms/data-access';
import { DotConfigurationVariables } from '@dotcms/dotcms-models';

import {
    DotPaletteStore,
    EditEmaPaletteStoreStatus,
    PALETTE_PAGINATOR_ITEMS_PER_PAGE,
    PALETTE_TYPES
} from './edit-ema-palette.store';

const WIDGET_MOCK = {
    baseType: 'WIDGET',
    id: 'widgetTest1',
    name: 'widgetTest1',
    variable: 'widgetTest1'
};

const VALID_CONTENT_TYPE_MOCK = {
    baseType: 'someContent',
    id: 'contentTypeTest1',
    name: 'contentTypeTest1',
    variable: 'contentTypeTest1'
};

describe('EditEmaPaletteStore', () => {
    let spectator: SpectatorService<DotPaletteStore>;
    let dotPropertiesService: DotPropertiesService;
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
                            VALID_CONTENT_TYPE_MOCK,
                            {
                                baseType: 'WIDGET',
                                id: 'contentTypeTest2',
                                name: 'contentTypeTest2',
                                variable: 'contentTypeTest2'
                            }
                        ]),
                    getContentTypes: () => of([WIDGET_MOCK])
                }
            },
            {
                provide: DotPropertiesService,
                useValue: {
                    getKeyAsList: () => of([])
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotPropertiesService = spectator.inject(DotPropertiesService);
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

            const getKeyAsListSpy = jest.spyOn(dotPropertiesService, 'getKeyAsList');
            spectator.service.loadContentTypes({
                filter: '',
                allowedContent: ['contentTypeTest1']
            });
            spectator.service.vm$.subscribe((state) => {
                expect(state.contenttypes.items).toEqual([VALID_CONTENT_TYPE_MOCK, WIDGET_MOCK]);
                expect(patchStateSpy).toHaveBeenCalled();
                expect(filterContentTypesSpy).toHaveBeenCalledWith('', 'contentTypeTest1');
                expect(getKeyAsListSpy).toHaveBeenCalledWith(
                    DotConfigurationVariables.CONTENT_PALETTE_HIDDEN_CONTENT_TYPES
                );
                expect(getContentTypesSpy).toHaveBeenCalledWith({
                    filter: '',
                    page: 40,
                    type: 'WIDGET'
                });
                done();
            });
        });

        it('should load content types with filter hidden content types', (done) => {
            const getKeyAsListSpy = jest
                .spyOn(dotPropertiesService, 'getKeyAsList')
                .mockReturnValue(of(['contentTypeTest1']));

            const payload = {
                filter: '',
                allowedContent: ['contentTypeTest1']
            };

            spectator.service.loadContentTypes(payload);
            spectator.service.vm$.subscribe((state) => {
                expect(state.contenttypes.items).toEqual([WIDGET_MOCK]);
                expect(getKeyAsListSpy).toHaveBeenCalledWith(
                    DotConfigurationVariables.CONTENT_PALETTE_HIDDEN_CONTENT_TYPES
                );
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

        it('should refresh contentlets', (done) => {
            // First set up the store state with existing filter data
            spectator.service.patchState({
                contentlets: {
                    filter: {
                        query: 'test query',
                        contentTypeVarName: 'TestContentType1'
                    },
                    items: [],
                    totalRecords: 0,
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                }
            });

            const contentServiceSpy = jest.spyOn(spectator.inject(DotESContentService), 'get');
            const setStatusSpy = jest.spyOn(spectator.service, 'setStatus');

            // Clear previous calls
            contentServiceSpy.mockClear();
            setStatusSpy.mockClear();

            spectator.service.refreshContentlets({
                languageId: 2,
                variantId: 'test-variant'
            });

            spectator.service.vm$.subscribe((state) => {
                expect(setStatusSpy).toHaveBeenCalledWith(EditEmaPaletteStoreStatus.LOADING);
                expect(contentServiceSpy).toHaveBeenCalledWith({
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE,
                    lang: '2',
                    filter: 'test query',
                    offset: '0',
                    query: '+contentType:TestContentType1 +deleted:false +variant:(DEFAULT OR test-variant)'
                });
                expect(state.status).toEqual(EditEmaPaletteStoreStatus.LOADED);
                done();
            });
        });
    });

    describe('selectors', () => {
        it('should return true for isContentTypeView$ when currentPaletteType is CONTENTTYPE', (done) => {
            spectator.service.patchState({
                currentPaletteType: PALETTE_TYPES.CONTENTTYPE
            });

            spectator.service.isContentTypeView$.subscribe((isContentTypeView) => {
                expect(isContentTypeView).toBe(true);
                done();
            });
        });

        it('should return false for isContentTypeView$ when currentPaletteType is not CONTENTTYPE', (done) => {
            spectator.service.patchState({
                currentPaletteType: PALETTE_TYPES.CONTENTLET
            });

            spectator.service.isContentTypeView$.subscribe((isContentTypeView) => {
                expect(isContentTypeView).toBe(false);
                done();
            });
        });
    });
});
