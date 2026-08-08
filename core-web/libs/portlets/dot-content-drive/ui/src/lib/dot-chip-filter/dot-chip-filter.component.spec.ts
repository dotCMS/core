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

    describe('outputs', () => {
        it('should emit clicked on host click', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            spectator.click(spectator.element);
            expect(handler).toHaveBeenCalled();
        });

        it('should emit clicked on Enter keydown', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            spectator.dispatchKeyboardEvent(spectator.element, 'keydown', 'Enter');
            expect(handler).toHaveBeenCalled();
        });

        it('should emit clicked on Space keydown', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            spectator.dispatchKeyboardEvent(spectator.element, 'keydown', ' ');
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

    describe('accessibility', () => {
        it('should expose role=button and tabindex=0 on the host', () => {
            expect(spectator.element.getAttribute('role')).toBe('button');
            expect(spectator.element.getAttribute('tabindex')).toBe('0');
        });

        it('should label the close button with the remove translation', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const removeBtn = spectator.query(byTestId('chip-remove'));
            expect(removeBtn?.getAttribute('aria-label')).toBe('Remove');
        });
    });
});
