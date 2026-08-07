import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DEFAULT_SEARCH_DEBOUNCE } from './constants';
import { DotSearchInputComponent } from './dot-search-input.component';

describe('DotSearchInputComponent', () => {
    let spectator: Spectator<DotSearchInputComponent>;

    const createComponent = createComponentFactory({
        component: DotSearchInputComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    search: 'Search',
                    'content-drive.search.placeholder': 'Search assets'
                })
            }
        ]
    });

    const getInput = () => spectator.query(byTestId('search-input-field')) as HTMLInputElement;
    const type = (value: string) => {
        spectator.typeInElement(value, getInput());
        spectator.detectChanges();
    };

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('placeholder', () => {
        it('should translate the default placeholder key', () => {
            expect(getInput().placeholder).toBe('Search');
        });

        it('should translate a custom placeholder key', () => {
            spectator.setInput('placeholder', 'content-drive.search.placeholder');
            spectator.detectChanges();

            expect(getInput().placeholder).toBe('Search assets');
        });
    });

    describe('debounced emission', () => {
        it('should not emit before the debounce window closes', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE - 1);

            expect(handler).not.toHaveBeenCalled();
        });

        it('should emit the term once the debounce window closes', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).toHaveBeenCalledWith('blog');
        });

        it('should emit only the last term typed within the window', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('b');
            jest.advanceTimersByTime(100);
            type('bl');
            jest.advanceTimersByTime(100);
            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).toHaveBeenCalledTimes(1);
            expect(handler).toHaveBeenCalledWith('blog');
        });

        it('should honor a custom debounceTime', () => {
            spectator.setInput('debounceTime', 50);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(50);

            expect(handler).toHaveBeenCalledWith('blog');
        });

        it('should trim the emitted term', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('  blog  ');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).toHaveBeenCalledWith('blog');
        });

        it('should not re-emit a term the host already has', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);
            type('blog ');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).toHaveBeenCalledTimes(1);
        });
    });

    describe('value input', () => {
        it('should seed the control from the host', () => {
            spectator.setInput('value', 'blog');
            spectator.detectChanges();

            expect(getInput().value).toBe('blog');
        });

        it('should not echo an emission back when the host pushes a value', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            spectator.setInput('value', 'blog');
            spectator.detectChanges();
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).not.toHaveBeenCalled();
        });

        it('should emit again after the host clears a term the user re-types', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            // Host echoes the term back, then clears the filter (e.g. "clear all")
            spectator.setInput('value', 'blog');
            spectator.detectChanges();
            spectator.setInput('value', '');
            spectator.detectChanges();

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(handler).toHaveBeenNthCalledWith(1, 'blog');
            expect(handler).toHaveBeenNthCalledWith(2, 'blog');
        });
    });

    describe('clear icon', () => {
        it('should be hidden while the input is empty', () => {
            expect(spectator.query(byTestId('search-icon-clear'))).toBeNull();
        });

        it('should appear as soon as the user types, without waiting for the debounce', () => {
            type('blog');

            expect(spectator.query(byTestId('search-icon-clear'))).not.toBeNull();
        });

        it('should clear the input and emit an empty term', () => {
            const handler = jest.fn();
            spectator.output('search').subscribe(handler);

            type('blog');
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            spectator.click(byTestId('search-icon-clear'));
            spectator.detectChanges();
            jest.advanceTimersByTime(DEFAULT_SEARCH_DEBOUNCE);

            expect(getInput().value).toBe('');
            expect(handler).toHaveBeenLastCalledWith('');
        });
    });
});
