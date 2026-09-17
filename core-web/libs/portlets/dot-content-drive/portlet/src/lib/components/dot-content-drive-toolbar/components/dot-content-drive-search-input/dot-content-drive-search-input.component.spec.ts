import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';
import { ZIndexUtils } from 'primeng/utils';

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
                getFilterValue: vi.fn().mockReturnValue(undefined),
                setGlobalSearch: vi.fn(),
                selectRootNode: vi.fn(),
                setSearchScope: vi.fn()
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

    afterEach(() => vi.clearAllMocks());

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
    describe('search scope', () => {
        // Absent from the filters means the default. The key is deliberately NOT `title` — that
        // holds the search TERM, and a scope whose value is 'TITLE' beside it would be a collision.
        const withScope = (scope?: string) =>
            store.getFilterValue.mockImplementation((key: string) =>
                key === 'searchScope' ? scope : undefined
            );

        it('should render the scope control next to the search input', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('search-scope-trigger'))).toBeTruthy();
            expect(searchInput()).toBeTruthy();
        });

        it('should show the active scope on the trigger', () => {
            withScope('TITLE');
            spectator.detectChanges();

            expect(spectator.component['$activeScopeLabel']()).toBe(
                'content-drive.search.scope.title'
            );
        });

        it('should start on All Fields when nothing is stored', () => {
            withScope(undefined);
            spectator.detectChanges();

            expect(spectator.component['$searchScope']()).toBe('ALL_FIELDS');
        });

        it('should read the stored scope when one is set', () => {
            withScope('TITLE');
            spectator.detectChanges();

            expect(spectator.component['$searchScope']()).toBe('TITLE');
        });

        // The placeholder deliberately does NOT describe the active scope — it is always the
        // shared component's own default ("Search"), so no [placeholder] binding is passed at all.
        // Regression guard for both directions: scope changes must not reintroduce one.
        it('should leave the input on its default placeholder in Title scope', () => {
            withScope('TITLE');
            spectator.detectChanges();

            expect(searchInput().componentInstance.$placeholder()).toBe('search');
        });

        it('should leave the input on its default placeholder in All Fields scope', () => {
            withScope(undefined);
            spectator.detectChanges();

            expect(searchInput().componentInstance.$placeholder()).toBe('search');
        });

        it('should record a newly chosen scope', () => {
            withScope(undefined);
            spectator.detectChanges();

            spectator.component['onScopeChange']('TITLE');

            expect(store.setSearchScope).toHaveBeenCalledWith('TITLE');
        });

        it('should ignore re-selecting the scope that is already active', () => {
            withScope('TITLE');
            spectator.detectChanges();

            spectator.component['onScopeChange']('TITLE');

            // Re-running cannot change the results, and it would reset the user to page 1.
            expect(store.setSearchScope).not.toHaveBeenCalled();
        });

        // p-listbox is single-select with metaKeySelection=false, so re-clicking the already
        // active option TOGGLES it and emits `null` via (ngModelChange) instead of the option's
        // value. Before the null guard, that null slipped past the "already active" check (`null
        // !== 'TITLE'`) and reached the store as a real scope change — silently dropping the user
        // back to All Fields and marking a clean drive as filtered (issue #37554 review).
        it('should ignore a null emission from re-clicking the active scope in the listbox', () => {
            withScope('TITLE');
            spectator.detectChanges();

            spectator.component['onScopeChange'](null);

            expect(store.setSearchScope).not.toHaveBeenCalled();
        });

        it('should name the control for assistive technology', () => {
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('search-scope-trigger'))?.getAttribute('aria-label')
            ).toBeTruthy();
        });

        // Lara centers a button's content, so label and chevron recentered as one group every time
        // the active scope changed — "Title" and "All Fields" rendered at different offsets.
        // `TRIGGER_PT` pins them to the button's edges instead; asserted through the rendered
        // inline style, so a lost PT slot fails here rather than in QA.
        it('should keep the trigger label and chevron in place when the scope changes', () => {
            spectator.detectChanges();

            const trigger = spectator.query(byTestId('search-scope-trigger')) as HTMLElement;

            expect(trigger.style.justifyContent).toBe('space-between');
        });

        it('should offer a distinct explanation for each option in the panel', () => {
            spectator.detectChanges();

            // Two labels do not carry the distinction between "the item's name" and "anything
            // written inside it", and the control is new. The explanation lives on each option
            // row in the panel, not on the trigger — asserted through the directive instances
            // rather than an ng-reflect attribute, which Angular only emits in development mode.
            spectator.click(byTestId('search-scope-trigger'));
            spectator.detectChanges();

            const tooltips = spectator.queryAll(Tooltip);

            expect(tooltips.length).toBe(2);
            expect(tooltips.every((tooltip) => !!tooltip.content)).toBe(true);

            const contents = tooltips.map((tooltip) => tooltip.content);

            expect(new Set(contents).size).toBe(2);
        });
    });

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

        /**
         * Review finding: this is the base-layer box, and the shell's own dialogs (Action Center,
         * folder and content-type selectors, upload) claim neither combination. Without standing
         * down, the shortcut pulls focus to the search field *behind* an open modal. Escape and the
         * tree toggle already decline the same way, for the same reason.
         *
         * `getCurrent` is mocked rather than trusted: the z-index stack is a module-level singleton
         * and an earlier test in the file can leave an entry behind.
         */
        it('should decline while an overlay is above the listing', () => {
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(1101);
            spectator.detectChanges();

            const event = pressSlash();

            expect(document.activeElement).not.toBe(input());
            expect(event.defaultPrevented).toBe(false);
        });

        // The modifier form carries no typing guard, so it reaches the handler from anywhere inside
        // an open dialog. It has to stand down too.
        it('should decline the alias while an overlay is above the listing', () => {
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(1101);
            spectator.detectChanges();

            const event = pressModK();

            expect(document.activeElement).not.toBe(input());
            // Both halves of standing down: the focus stays put *and* the key is left for whatever
            // is above to use. Asserting only the first would pass while the key was swallowed.
            expect(event.defaultPrevented).toBe(false);
        });

        it('should resume once the overlay closes', () => {
            const stack = vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(1101);
            spectator.detectChanges();
            pressSlash();

            stack.mockReturnValue(0);
            pressSlash();

            expect(document.activeElement).toBe(input());
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
