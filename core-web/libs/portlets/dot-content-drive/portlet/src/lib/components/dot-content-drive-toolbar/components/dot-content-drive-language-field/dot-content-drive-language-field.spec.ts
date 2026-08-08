import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguageFilterComponent } from '@dotcms/ui';
import { createFakeLanguage, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveLanguageFieldComponent } from './dot-content-drive-language-field.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveLanguageFieldComponent', () => {
    let spectator: Spectator<DotContentDriveLanguageFieldComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveLanguageFieldComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn()
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of([createFakeLanguage({ id: 1 })]))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.language-selector.placeholder': 'Language',
                    search: 'Search'
                })
            }
        ],
        detectChanges: false
    });

    const languageFilter = () =>
        spectator.fixture.debugElement.query(By.directive(DotLanguageFilterComponent));

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        store.getFilterValue.mockReset().mockReturnValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('should render the shared language filter', () => {
        spectator.detectChanges();

        expect(languageFilter()).toBeTruthy();
    });

    it('should bind the store languageId filter as numbers', () => {
        store.getFilterValue.mockReturnValue(['1', '2']);
        spectator.detectChanges();

        expect(languageFilter().componentInstance.$selectedLanguageIds()).toEqual([1, 2]);
        expect(store.getFilterValue).toHaveBeenCalledWith('languageId');
    });

    it('should bind an empty selection when no languageId filter is set', () => {
        spectator.detectChanges();

        expect(languageFilter().componentInstance.$selectedLanguageIds()).toEqual([]);
    });

    it('should patch the store with string ids when a selection is emitted', () => {
        spectator.detectChanges();

        spectator.triggerEventHandler(languageFilter(), 'selectionChange', [1, 2]);

        expect(store.patchFilters).toHaveBeenCalledWith({ languageId: ['1', '2'] });
    });

    it('should remove the filter when an empty selection is emitted', () => {
        store.getFilterValue.mockReturnValue(['1']);
        spectator.detectChanges();

        spectator.triggerEventHandler(languageFilter(), 'selectionChange', []);

        expect(store.removeFilter).toHaveBeenCalledWith('languageId');
        expect(store.patchFilters).not.toHaveBeenCalled();
    });
});
