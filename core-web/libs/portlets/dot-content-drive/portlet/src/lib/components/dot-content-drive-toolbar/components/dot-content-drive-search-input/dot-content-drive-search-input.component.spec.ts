import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotSearchInputComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveSearchInputComponent } from './dot-content-drive-search-input.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveSearchInputComponent', () => {
    let spectator: Spectator<DotContentDriveSearchInputComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveSearchInputComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                setGlobalSearch: jest.fn(),
                selectRootNode: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ search: 'Search' })
            }
        ],
        detectChanges: false
    });

    const searchInput = () =>
        spectator.fixture.debugElement.query(By.directive(DotSearchInputComponent));

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        store.getFilterValue.mockReset().mockReturnValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('should render the shared search input', () => {
        spectator.detectChanges();

        expect(searchInput()).toBeTruthy();
    });

    it('should bind the store title filter as the value', () => {
        store.getFilterValue.mockReturnValue('blog');
        spectator.detectChanges();

        expect(searchInput().componentInstance.$value()).toBe('blog');
        expect(store.getFilterValue).toHaveBeenCalledWith('title');
    });

    it('should bind an empty value when no title filter is set', () => {
        spectator.detectChanges();

        expect(searchInput().componentInstance.$value()).toBe('');
    });

    it('should push the emitted term to the store and reset the folder scope', () => {
        spectator.detectChanges();

        spectator.triggerEventHandler(searchInput(), 'search', 'blog');

        expect(store.setGlobalSearch).toHaveBeenCalledWith('blog');
        expect(store.selectRootNode).toHaveBeenCalled();
    });

    it('should clear the search in the store when an empty term is emitted', () => {
        store.getFilterValue.mockReturnValue('blog');
        spectator.detectChanges();

        spectator.triggerEventHandler(searchInput(), 'search', '');

        expect(store.setGlobalSearch).toHaveBeenCalledWith('');
        expect(store.selectRootNode).toHaveBeenCalled();
    });

    // The claim lives here rather than in the shell because this component is the one holding the
    // search box; the shell would have to reach three levels down to find it.
    describe('search shortcut', () => {
        /** Dispatches from `target` (the document unless a field is given), the way a browser would. */
        const press = (init: KeyboardEventInit, target: EventTarget = document): KeyboardEvent => {
            const event = new KeyboardEvent('keydown', {
                bubbles: true,
                cancelable: true,
                ...init
            });
            target.dispatchEvent(event);

            return event;
        };

        const pressSlash = (target?: EventTarget) => press({ key: '/' }, target);
        const pressModK = (target?: EventTarget) => press({ key: 'k', metaKey: true }, target);

        const input = () => spectator.query('[data-testid="search-input-field"]');

        it('should focus the search field', () => {
            spectator.detectChanges();

            pressSlash();

            expect(document.activeElement).toBe(input());
        });

        // Kept as an alias for the habit, so both keys have to work.
        it('should focus the search field from the alias too', () => {
            spectator.detectChanges();

            pressModK();

            expect(document.activeElement).toBe(input());
        });

        it('should preserve a term already entered', () => {
            store.getFilterValue.mockReturnValue('blog');
            spectator.detectChanges();

            pressSlash();

            expect((input() as HTMLInputElement).value).toBe('blog');
        });

        // The slash is the one that has to hold: some browsers open a quick-find bar on it, which
        // would steal the keystroke and the focus the shortcut just placed.
        it('should suppress the browser default so no quick-find bar opens', () => {
            spectator.detectChanges();

            const event = pressSlash();

            expect(event.defaultPrevented).toBe(true);
        });

        it('should suppress the browser default for the alias too', () => {
            spectator.detectChanges();

            const event = pressModK();

            expect(event.defaultPrevented).toBe(true);
        });

        // The reason a bare printable key needs the registry's typing rule: without it, typing a
        // slash into the very box the shortcut focuses would re-fire the shortcut instead of
        // entering a character.
        it('should let a slash typed into the search field through as text', () => {
            spectator.detectChanges();

            const event = pressSlash(input() as HTMLElement);

            expect(event.defaultPrevented).toBe(false);
        });

        it('should release the claim when the component is destroyed', () => {
            spectator.detectChanges();
            const field = input();

            spectator.fixture.destroy();
            press();

            expect(document.activeElement).not.toBe(field);
        });
    });
});
