import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { discardPeriodicTasks, fakeAsync } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import {
    DEBOUNCE_TIME,
    DotCategoryFieldSearchComponent
} from './dot-category-field-search.component';

const TERM_TO_SEARCH = 'Wood';
describe('DotCategoryFieldSearchComponent', () => {
    let spectator: Spectator<DotCategoryFieldSearchComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSearchComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        spectator.setInput('isLoading', false);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should show only the search icon', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('search-icon-search'))).toBeTruthy();
        expect(spectator.query(byTestId('search-icon-clear'))).not.toBeTruthy();
        expect(spectator.query(byTestId('search-icon-loading'))).not.toBeTruthy();
    });

    it('should emit "term" with correct value on input change', fakeAsync(() => {
        const termSpy = jest.spyOn(spectator.component.term, 'emit');
        const input = spectator.query(byTestId('search-input')) as HTMLInputElement;

        spectator.typeInElement(TERM_TO_SEARCH, input);

        spectator.tick(DEBOUNCE_TIME + 100);

        expect(termSpy).toHaveBeenCalledWith(TERM_TO_SEARCH);

        discardPeriodicTasks();
    }));

    it('should clear input and emit "changeMode" when clear icon is clicked', fakeAsync(() => {
        const changeModeSpy = jest.spyOn(spectator.component.changeMode, 'emit');
        const input = spectator.query(byTestId('search-input')) as HTMLInputElement;
        spectator.typeInElement(TERM_TO_SEARCH, input);
        spectator.tick(DEBOUNCE_TIME + 100);

        spectator.detectChanges();

        spectator.click(spectator.query(byTestId('search-icon-clear')));

        expect(input.value).toBe('');
        expect(changeModeSpy).toHaveBeenCalledWith('list');
        discardPeriodicTasks();
    }));

    it('should show loading icon when isLoading is true', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();

        expect(spectator.query(byTestId('search-icon-clear'))).not.toBeTruthy();
        expect(spectator.query(byTestId('search-icon-loading'))).toBeTruthy();
    });

    it('should show clear icon when there is input and not loading', fakeAsync(() => {
        const input = spectator.query(byTestId('search-input')) as HTMLInputElement;
        spectator.typeInElement('search term', input);
        spectator.tick(DEBOUNCE_TIME + 100);

        spectator.setInput('isLoading', false);
        spectator.detectChanges();

        const clearIcon = spectator.query(byTestId('search-icon-clear'));
        expect(clearIcon).toBeTruthy();

        discardPeriodicTasks();
    }));
});
