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
                defaultLanguageId: jest.fn().mockReturnValue(1),
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

    describe('removable', () => {
        // The shared filter's spec pins that the input suppresses the chip's X. These pin the wiring:
        // that this adapter computes it from the store's default and passes it down. Bug 5's
        // affordance was lost once already, when the component this logic lived in was deleted by a
        // refactor upstream, so both halves are guarded separately.
        it('should not offer removal while the only selection is the environment default', () => {
            // Clearing it would re-seed the very same value, so the X would do nothing visible.
            store.getFilterValue.mockReturnValue(['1']);
            spectator.detectChanges();

            expect(languageFilter().componentInstance.$removable()).toBe(false);
        });

        it('should offer removal once a non-default language is selected', () => {
            store.getFilterValue.mockReturnValue(['2']);
            spectator.detectChanges();

            expect(languageFilter().componentInstance.$removable()).toBe(true);
        });

        it('should offer removal when the default is selected alongside another', () => {
            // Two selections means clearing genuinely changes what is filtered.
            store.getFilterValue.mockReturnValue(['1', '2']);
            spectator.detectChanges();

            expect(languageFilter().componentInstance.$removable()).toBe(true);
        });

        it('should offer removal when nothing is selected yet', () => {
            spectator.detectChanges();

            expect(languageFilter().componentInstance.$removable()).toBe(true);
        });

        it('should track the environment default rather than hardcoding an id', () => {
            // A different default must move the behaviour with it.
            store.defaultLanguageId.mockReturnValue(2);
            store.getFilterValue.mockReturnValue(['2']);
            spectator.detectChanges();

            expect(languageFilter().componentInstance.$removable()).toBe(false);
        });
    });

    it('should remove the filter when an empty selection is emitted', () => {
        store.getFilterValue.mockReturnValue(['1']);
        spectator.detectChanges();

        spectator.triggerEventHandler(languageFilter(), 'selectionChange', []);

        expect(store.removeFilter).toHaveBeenCalledWith('languageId');
        expect(store.patchFilters).not.toHaveBeenCalled();
    });
});
