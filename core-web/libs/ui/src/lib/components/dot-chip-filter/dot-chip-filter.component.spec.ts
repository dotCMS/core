import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotChipFilterComponent } from './dot-chip-filter.component';

describe('DotChipFilterComponent', () => {
    let spectator: Spectator<DotChipFilterComponent>;

    const createComponent = createComponentFactory({
        component: DotChipFilterComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.chip-filter.overflow-label': '{0} and {1} more',
                    'dot.common.remove': 'Remove'
                })
            }
        ]
    });

    const getTitle = () => spectator.query(byTestId('chip-title'))?.textContent?.trim();
    const getValues = () => spectator.query(byTestId('chip-values'))?.textContent?.trim();

    beforeEach(() => {
        spectator = createComponent({ props: { title: 'Type' } });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('label', () => {
        it('should always render the title', () => {
            expect(getTitle()).toBe('Type');
        });

        it('should not render the values span when there are no selections', () => {
            expect(spectator.query(byTestId('chip-values'))).toBeFalsy();
        });

        it('should render the values span when there is at least one selection', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.query(byTestId('chip-values'))).toBeTruthy();
        });

        it('should render one selection', () => {
            spectator.setInput('selections', ['Blog']);
            expect(getValues()).toBe(': Blog');
        });

        it('should render two selections joined by comma', () => {
            spectator.setInput('selections', ['Blog', 'Activities']);
            expect(getValues()).toBe(': Blog, Activities');
        });

        it.each([
            [['Blog', 'News', 'Events'], ': Blog and 2 more'],
            [['Blog', 'News', 'Events', 'Sports'], ': Blog and 3 more']
        ])(
            'should render first selection and remaining count for %s',
            (selections: string[], expected: string) => {
                spectator.setInput('selections', selections);
                expect(getValues()).toBe(expected);
            }
        );
    });

    describe('active state', () => {
        it('should show chevron-down icon when there are no selections', () => {
            expect(spectator.query('.pi-chevron-down')).toBeTruthy();
            expect(spectator.query('.pi-times')).toBeFalsy();
        });

        it('should show close icon when there are selections', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.query('.pi-times')).toBeTruthy();
            expect(spectator.query('.pi-chevron-down')).toBeFalsy();
        });
    });

    describe('toggle mode with an empty label', () => {
        it('should not show the empty label while the toggle is on', () => {
            // The two features met in the same @if chain during a merge: a toggle never matches the
            // values branch, so an unguarded @else would render the "nothing selected" label on an
            // ACTIVE toggle.
            spectator.setInput('mode', 'toggle');
            spectator.setInput('emptyLabel', 'All');
            spectator.setInput('toggled', true);

            expect(spectator.query(byTestId('chip-empty-label'))).toBeFalsy();
        });

        it('should not show the empty label while the toggle is off either', () => {
            spectator.setInput('mode', 'toggle');
            spectator.setInput('emptyLabel', 'All');
            spectator.setInput('toggled', false);

            expect(spectator.query(byTestId('chip-empty-label'))).toBeFalsy();
        });

        it('should still show the empty label on a dropdown chip with no selection', () => {
            spectator.setInput('emptyLabel', 'All');
            spectator.setInput('selections', []);

            expect(spectator.query(byTestId('chip-empty-label'))?.textContent).toContain('All');
        });
    });

    describe('removable', () => {
        it('should hide the remove button when not removable, keeping the selection visible', () => {
            // A filter that always holds a value (the Locale chip on its environment default) has
            // nothing to remove — clearing it would re-select the very same value.
            spectator.setInput('selections', ['English (en-US)']);
            spectator.setInput('removable', false);

            expect(spectator.query(byTestId('chip-remove'))).toBeFalsy();
            expect(spectator.query('.pi-chevron-down')).toBeTruthy();
            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain(
                'English (en-US)'
            );
        });

        it('should show the remove button once removable', () => {
            spectator.setInput('selections', ['English (en-US)', 'Spanish (es-ES)']);
            spectator.setInput('removable', true);

            expect(spectator.query(byTestId('chip-remove'))).toBeTruthy();
        });
    });

    describe('outputs', () => {
        it('should emit clicked on host click', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            spectator.click(spectator.element);
            expect(handler).toHaveBeenCalled();
        });

        // `spectator.dispatchKeyboardEvent` builds the event with the legacy
        // `initKeyboardEvent`, which happy-dom (this lib's test environment) does not
        // implement. A real KeyboardEvent works under both happy-dom and jsdom.
        const pressKey = (key: string) =>
            spectator.element.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));

        it('should emit clicked on Enter keydown', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            pressKey('Enter');
            expect(handler).toHaveBeenCalled();
        });

        it('should emit clicked on Space keydown', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            pressKey(' ');
            expect(handler).toHaveBeenCalled();
        });

        it('should emit removed when the close button is clicked', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('removed').subscribe(handler);
            spectator.click(byTestId('chip-remove'));
            expect(handler).toHaveBeenCalled();
        });

        it('should not emit clicked when the close button is clicked', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const clickedHandler = jest.fn();
            spectator.output('clicked').subscribe(clickedHandler);
            spectator.click(byTestId('chip-remove'));
            expect(clickedHandler).not.toHaveBeenCalled();
        });

        it('should not emit clicked when Enter is pressed on the close button', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const clickedHandler = jest.fn();
            spectator.output('clicked').subscribe(clickedHandler);

            const removeBtn = spectator.query(byTestId('chip-remove')) as HTMLElement;
            expect(removeBtn).toBeTruthy();
            removeBtn.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

            expect(clickedHandler).not.toHaveBeenCalled();
        });

        it('should not emit clicked when Space is pressed on the close button', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const clickedHandler = jest.fn();
            spectator.output('clicked').subscribe(clickedHandler);

            const removeBtn = spectator.query(byTestId('chip-remove')) as HTMLElement;
            expect(removeBtn).toBeTruthy();
            removeBtn.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));

            expect(clickedHandler).not.toHaveBeenCalled();
        });
    });

    describe('toggle mode', () => {
        beforeEach(() => {
            spectator.setInput('mode', 'toggle');
            spectator.setInput('title', 'Show shared assets');
        });

        it('should take its state from `toggled` rather than from selections', () => {
            spectator.setInput('toggled', true);

            expect(spectator.query(byTestId('chip-remove'))).toBeTruthy();
        });

        it('should read as off while `toggled` is false, even with selections passed', () => {
            spectator.setInput('toggled', false);
            spectator.setInput('selections', ['ignored']);

            expect(spectator.query(byTestId('chip-remove'))).toBeFalsy();
        });

        it('should render no values label, since being on is the whole state', () => {
            spectator.setInput('toggled', true);
            spectator.setInput('selections', ['ignored']);

            expect(spectator.query(byTestId('chip-values'))).toBeFalsy();
            expect(getTitle()).toBe('Show shared assets');
        });

        it('should offer the remove control while on, because it turns the toggle off', () => {
            spectator.setInput('toggled', true);

            expect(spectator.query('.pi-times')).toBeTruthy();
            expect(spectator.query('.pi-chevron-down')).toBeFalsy();
        });

        it('should render no trailing affordance while off: there is no overlay and nothing to clear', () => {
            spectator.setInput('toggled', false);

            expect(spectator.query('.pi-times')).toBeFalsy();
            expect(spectator.query('.pi-chevron-down')).toBeFalsy();
        });

        it('should emit removed when the remove control is clicked', () => {
            spectator.setInput('toggled', true);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('removed').subscribe(handler);
            spectator.click(byTestId('chip-remove'));

            expect(handler).toHaveBeenCalled();
        });

        it.each([
            [true, 'true'],
            [false, 'false']
        ])('should report aria-pressed=%s as "%s"', (toggled: boolean, expected: string) => {
            spectator.setInput('toggled', toggled);

            expect(spectator.element.getAttribute('aria-pressed')).toBe(expected);
        });
    });

    describe('accessibility', () => {
        it('should expose role=button and tabindex=0 on the host', () => {
            expect(spectator.element.getAttribute('role')).toBe('button');
            expect(spectator.element.getAttribute('tabindex')).toBe('0');
        });

        it('should not claim a pressed state in dropdown mode, whose state lives in its overlay', () => {
            spectator.setInput('selections', ['Blog']);

            expect(spectator.element.getAttribute('aria-pressed')).toBeNull();
        });

        it('should label the close button with the remove translation', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const removeBtn = spectator.query(byTestId('chip-remove'));
            expect(removeBtn?.getAttribute('aria-label')).toBe('Remove');
        });
    });
});
