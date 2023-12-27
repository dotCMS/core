import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';

import { EditEmaPaletteContentTypeComponent } from './components/edit-ema-palette-content-type/edit-ema-palette-content-type.component';
import { EditEmaPaletteContentletsComponent } from './components/edit-ema-palette-contentlets/edit-ema-palette-contentlets.component';
import { EditEmaPaletteComponent } from './edit-ema-palette.component';
import {
    CONTENTLETS_MOCK,
    INITIAL_STATE_PALETTE_CONTENTLET_MOCK,
    INITIAL_STATE_PALETTE_CONTENTTYPE_MOCK
} from './shared/edit-ema-palette.mocks';
import { DotPaletteStore, PALETTE_TYPES } from './store/edit-ema-palette.store';

const createEditEmaPaletteComponent = () => {
    return createComponentFactory({
        component: EditEmaPaletteComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotContentTypeService,
                useValue: {
                    filterContentTypes: () => of([]),
                    getContentTypes: () => of([])
                }
            },
            {
                provide: DotESContentService,
                useValue: {
                    get: () =>
                        of({
                            jsonObjectView: { contentlets: CONTENTLETS_MOCK },
                            resultSize: CONTENTLETS_MOCK.length
                        })
                }
            }
        ]
    });
};

describe('EditEmaPaletteComponent', () => {
    describe('ContentTypes', () => {
        let spectator: Spectator<EditEmaPaletteComponent>;
        let store: DotPaletteStore;
        const createComponent = createEditEmaPaletteComponent();

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    languageId: 1,
                    containers: {}
                },
                providers: [
                    {
                        provide: DotPaletteStore,
                        useValue: {
                            vm$: of(INITIAL_STATE_PALETTE_CONTENTTYPE_MOCK),
                            loadContentlets: () => of({}),
                            changeView: () => of({}),
                            resetContentlets: () => ({}),
                            loadAllowedContentTypes: () => ({})
                        }
                    }
                ]
            });
            store = spectator.inject(DotPaletteStore);
        });

        it('should render Content Types', () => {
            expect(spectator.query(EditEmaPaletteContentTypeComponent)).toBeDefined();
        });

        it('should not render Contentlets', () => {
            expect(spectator.query(EditEmaPaletteContentletsComponent)).toBeNull();
        });

        it('should emit dragStart event on drag start', () => {
            const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');

            spectator.triggerEventHandler(EditEmaPaletteContentTypeComponent, 'dragStart', {
                variable: 'test',
                name: 'Test'
            });

            expect(dragSpy).toHaveBeenCalledWith({
                variable: 'test',
                name: 'Test'
            });
        });

        it('should emit dragEnd event on drag end', () => {
            const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

            spectator.triggerEventHandler(EditEmaPaletteContentTypeComponent, 'dragEnd', {
                variable: 'test',
                name: 'Test'
            });
            expect(dragSpy).toHaveBeenCalledWith({
                variable: 'test',
                name: 'Test'
            });
        });

        it('should show contentlets from content type', () => {
            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(
                EditEmaPaletteContentTypeComponent,
                'showContentlets',
                'TestNameContentType'
            );
            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType'
            });
        });
    });

    describe('Contentlets', () => {
        let spectator: Spectator<EditEmaPaletteComponent>;
        let store: DotPaletteStore;
        const createComponent = createEditEmaPaletteComponent();

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    languageId: 1,
                    containers: {}
                },
                providers: [
                    {
                        provide: DotPaletteStore,
                        useValue: {
                            vm$: of(INITIAL_STATE_PALETTE_CONTENTLET_MOCK),
                            loadContentlets: () => of({}),
                            changeView: () => of({}),
                            resetContentlets: () => ({}),
                            loadAllowedContentTypes: () => ({})
                        }
                    }
                ]
            });
            store = spectator.inject(DotPaletteStore);
        });

        it('should load allowed contentTypes on init', () => {
            const storeSpy = jest.spyOn(store, 'loadAllowedContentTypes');
            spectator.component.ngOnInit();
            expect(storeSpy).toHaveBeenCalledWith({ containers: {} });
        });

        it('should render Contentlets', () => {
            expect(spectator.query(EditEmaPaletteContentletsComponent)).toBeDefined();
        });

        it('should not render ContentTypes', () => {
            expect(spectator.query(EditEmaPaletteContentTypeComponent)).toBeNull();
        });

        it('should load contentlets on paginate', () => {
            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(EditEmaPaletteContentletsComponent, 'paginate', {
                contentTypeVarName: 'TestNameContentType',
                page: 1
            });

            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType',
                page: 1
            });
        });

        it('should show content types when component emits click on back button', () => {
            const changeViewSpy = jest.spyOn(store, 'changeView');
            const resetContentLetsSpy = jest.spyOn(store, 'changeView');
            spectator.triggerEventHandler(
                EditEmaPaletteContentletsComponent,
                'showContentTypes',
                true
            );

            expect(changeViewSpy).toHaveBeenCalledWith(PALETTE_TYPES.CONTENTTYPE);
            expect(resetContentLetsSpy).toHaveBeenCalled();
        });

        it('should emit dragStart event on drag start', () => {
            const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');

            spectator.triggerEventHandler(EditEmaPaletteContentletsComponent, 'dragStart', {
                inode: '123'
            });
            expect(dragSpy).toHaveBeenCalledWith({
                inode: '123'
            });
        });

        it('should emit dragEnd event on drag end', () => {
            const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

            spectator.triggerEventHandler(EditEmaPaletteContentletsComponent, 'dragEnd', {
                inode: '123'
            });
            expect(dragSpy).toHaveBeenCalledWith({
                inode: '123'
            });
        });
    });
});
