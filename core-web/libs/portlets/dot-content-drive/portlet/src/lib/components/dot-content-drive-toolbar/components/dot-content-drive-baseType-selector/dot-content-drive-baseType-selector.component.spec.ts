import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { MultiSelect, MultiSelectChangeEvent } from 'primeng/multiselect';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';

import { DotContentDriveBaseTypeSelectorComponent } from './dot-content-drive-baseType-selector.component';

import { MOCK_BASE_TYPES } from '../../../../shared/mocks';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveBaseTypeSelectorComponent', () => {
    let spectator: Spectator<DotContentDriveBaseTypeSelectorComponent>;
    let component: DotContentDriveBaseTypeSelectorComponent;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveBaseTypeSelectorComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn()
            }),
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        store = spectator.inject(DotContentDriveStore, true);
        contentTypeService = spectator.inject(DotContentTypeService);
        store.getFilterValue.mockReturnValue([]);
    });

    it('should fetch content types and filter out FORM type', () => {
        spectator.detectChanges();

        expect(contentTypeService.getAllContentTypes).toHaveBeenCalled();
        expect(component.$state().baseTypes).toEqual([
            { name: 'Content', label: 'Content', types: null },
            { name: 'Widget', label: 'Widget', types: null },
            { name: 'FileAsset', label: 'FileAsset', types: null }
        ]);
    });

    it('should set selectedBaseTypes when store has baseType filter', () => {
        store.getFilterValue.mockReturnValue(['1', '2']);

        spectator.detectChanges();

        expect(store.getFilterValue).toHaveBeenCalledWith('baseType');
        expect(component.$selectedBaseTypes()).toEqual(['CONTENT', 'WIDGET']);
    });

    it('should patch filters with keys when selectedBaseTypes has values', () => {
        spectator.detectChanges();

        const multiSelectDebugElement = spectator.fixture.debugElement.query(
            By.directive(MultiSelect)
        );

        spectator.triggerEventHandler(multiSelectDebugElement, 'ngModelChange', [
            'CONTENT',
            'WIDGET'
        ]);

        expect(component.$selectedBaseTypes()).toEqual(['CONTENT', 'WIDGET']);

        spectator.triggerEventHandler(MultiSelect, 'onChange', {} as MultiSelectChangeEvent);

        expect(store.patchFilters).toHaveBeenCalledWith({
            baseType: ['1', '2']
        });
    });

    it('should remove filter when selectedBaseTypes is empty', () => {
        component.$selectedBaseTypes.set([]);

        const multiSelectDebugElement = spectator.fixture.debugElement.query(
            By.directive(MultiSelect)
        );

        spectator.triggerEventHandler(multiSelectDebugElement, 'ngModelChange', []);

        spectator.triggerEventHandler(MultiSelect, 'onChange', {} as MultiSelectChangeEvent);

        expect(store.removeFilter).toHaveBeenCalledWith('baseType');
    });
});
