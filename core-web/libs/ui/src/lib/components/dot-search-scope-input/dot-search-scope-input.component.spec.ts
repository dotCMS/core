import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotSearchScope } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSearchScopeInputComponent } from './dot-search-scope-input.component';

import { DotSearchInputComponent } from '../dot-search-input/dot-search-input.component';

/**
 * The search box with the Title / All Fields scope, shared by Content Drive and the AssetPicker.
 *
 * Only the message service is provided on purpose: the control has to work in a host that has no
 * Router, no Content Drive store and no shortcut registry (the AssetPicker also runs in a legacy
 * host), so a construction that needed any of them would fail here.
 */
describe('DotSearchScopeInputComponent', () => {
    let spectator: Spectator<DotSearchScopeInputComponent>;

    const createComponent = createComponentFactory({
        component: DotSearchScopeInputComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ search: 'Search' })
            }
        ],
        detectChanges: false
    });

    /**
     * Spectator types `props` by class member (`$term`) but sets them by public alias (`term`), so
     * the aliased inputs can only be passed through this cast.
     */
    const byAlias = (props: Record<string, unknown>) => props as never;

    const searchInput = () =>
        spectator.fixture.debugElement.query(By.directive(DotSearchInputComponent));

    // The panel is appended to the body, so one test's popover would still be there for the next.
    afterEach(() => document.querySelectorAll('.p-popover').forEach((panel) => panel.remove()));

    const trigger = () => spectator.query(byTestId('search-scope-trigger')) as HTMLElement;

    const open = (scope: DotSearchScope = 'TITLE', term = '') => {
        spectator = createComponent({ props: byAlias({ term, scope }) });
        spectator.detectChanges();
    };

    /** Opens the scope panel the way a user does. */
    const openPanel = () => {
        spectator.click(byTestId('search-scope-trigger'));
        spectator.detectChanges();
    };

    /** What the listbox emits when an option is picked (or `null` for a toggle-off click). */
    const pick = (value: DotSearchScope | null) =>
        spectator.triggerEventHandler('[data-testid="search-scope-panel"]', 'ngModelChange', value);

    describe('search box', () => {
        it('should render the shared search input with the term it is given', () => {
            open('TITLE', 'blog');

            expect(searchInput().componentInstance.$value()).toBe('blog');
        });

        it('should leave the input on its default placeholder in either scope', () => {
            open('ALL_FIELDS');

            expect(searchInput().componentInstance.$placeholder()).toBe('search');
        });

        it('should keep the default test id on the text box', () => {
            open();

            expect(spectator.query('[data-testid="search-input-field"]')).toBeTruthy();
        });

        it('should let the host name the text box, for hosts with more than one search box', () => {
            spectator = createComponent({
                props: byAlias({
                    term: '',
                    scope: 'TITLE',
                    testId: 'asset-picker-search-input'
                })
            });
            spectator.detectChanges();

            expect(spectator.query('[data-testid="asset-picker-search-input"]')).toBeTruthy();
        });

        it('should emit the debounced term from the shared input', () => {
            open();
            const searched = vi.fn();
            spectator.output('search').subscribe(searched);

            spectator.triggerEventHandler(searchInput(), 'search', 'blog');

            expect(searched).toHaveBeenCalledWith('blog');
        });

        it('should focus the text box on demand, for hosts that bind their own shortcut', () => {
            open();

            spectator.component.focus();

            expect(document.activeElement).toBe(
                spectator.query('[data-testid="search-input-field"]')
            );
        });
    });

    describe('scope trigger', () => {
        it('should show the active scope on the trigger: Title', () => {
            open('TITLE');

            expect(spectator.query(byTestId('search-scope-active'))?.textContent?.trim()).toBe(
                'content-drive.search.scope.title'
            );
        });

        it('should show the active scope on the trigger: All Fields', () => {
            open('ALL_FIELDS');

            expect(spectator.query(byTestId('search-scope-active'))?.textContent?.trim()).toBe(
                'content-drive.search.scope.all-fields'
            );
        });

        it('should follow the scope the host passes in', () => {
            open('TITLE');

            spectator.setInput('scope', 'ALL_FIELDS');

            expect(spectator.query(byTestId('search-scope-active'))?.textContent?.trim()).toBe(
                'content-drive.search.scope.all-fields'
            );
        });

        it('should name the control for assistive technology and announce a listbox', () => {
            open();

            expect(trigger().getAttribute('aria-label')).toBe('content-drive.search.scope.label');
            expect(trigger().getAttribute('aria-haspopup')).toBe('listbox');
        });

        it('should keep the label and chevron in place when the scope changes', () => {
            open();

            expect(trigger().style.justifyContent).toBe('space-between');
        });

        it('should paint the trigger with the field background', () => {
            open();

            expect(trigger().style.background).toBe('var(--p-inputtext-background)');
        });
    });

    describe('scope panel', () => {
        it('should offer exactly two options: Title and All Fields', () => {
            open();
            openPanel();

            expect(
                document.querySelector('[data-testid="search-scope-option-TITLE"]')
            ).toBeTruthy();
            expect(
                document.querySelector('[data-testid="search-scope-option-ALL_FIELDS"]')
            ).toBeTruthy();
            expect(document.querySelectorAll('[data-testid^="search-scope-option-"]')).toHaveLength(
                2
            );
        });

        it('should explain each option with its own tooltip', () => {
            open();
            openPanel();

            const tooltips = spectator.queryAll(Tooltip);

            expect(tooltips).toHaveLength(2);
            expect(new Set(tooltips.map((tooltip) => tooltip.content)).size).toBe(2);
        });

        it('should mark the active option with a checkmark', async () => {
            open('ALL_FIELDS');
            openPanel();
            // `ngModel` writes the listbox's value in a microtask, so wait for it before asking.
            await spectator.fixture.whenStable();

            // Asserted through the listbox instance, not the DOM it paints: the overlay is
            // body-appended and rendered after an animation, which makes a DOM query timing-sensitive.
            const listbox = spectator.query(Listbox) as Listbox;
            const [title, allFields] = listbox.options as { value: DotSearchScope }[];

            expect(listbox.checkmark).toBe(true);
            expect(listbox.isSelected(allFields)).toBe(true);
            expect(listbox.isSelected(title)).toBe(false);
        });
    });

    describe('scope changes', () => {
        it('should emit the new scope when a different one is picked', () => {
            open('TITLE');
            openPanel();
            const changed = vi.fn();
            spectator.output('scopeChange').subscribe(changed);

            pick('ALL_FIELDS');

            expect(changed).toHaveBeenCalledWith('ALL_FIELDS');
        });

        it('should ignore re-selecting the scope that is already active', () => {
            // Re-running cannot change the results, and it would send the host back to page 1.
            open('TITLE');
            openPanel();
            const changed = vi.fn();
            spectator.output('scopeChange').subscribe(changed);

            pick('TITLE');

            expect(changed).not.toHaveBeenCalled();
        });

        it('should ignore the null the listbox emits when the active option is clicked again', () => {
            // p-listbox is single-select and toggles: re-clicking the selected option emits `null`.
            // Passing it on would flip the host to All Fields behind the user's back.
            open('TITLE');
            openPanel();
            const changed = vi.fn();
            spectator.output('scopeChange').subscribe(changed);

            pick(null);

            expect(changed).not.toHaveBeenCalled();
        });
    });
});
