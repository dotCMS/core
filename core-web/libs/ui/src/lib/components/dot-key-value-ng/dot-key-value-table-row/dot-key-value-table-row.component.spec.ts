import { SpectatorHost, byTestId, createHostFactory } from '@openng/spectator/jest';

import { Table } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';

import { DotKeyValue } from '../dot-key-value-ng.component';

const mockVariable: DotKeyValue = { key: 'name', hidden: false, value: 'John' };

const messageServiceMock = new MockDotMessageService({
    'keyValue.value_input.placeholder': 'Enter Value',
    'keyValue.value_hidden': 'Value hidden',
    Delete: 'Delete',
    Reorder: 'Reorder'
});

describe('DotKeyValueTableRowComponent', () => {
    let spectator: SpectatorHost<DotKeyValueTableRowComponent>;

    const createHost = createHostFactory({
        component: DotKeyValueTableRowComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            // `pReorderableRowHandle` reaches for the parent Table through DI.
            { provide: Table, useValue: { onRowReorder: { emit: jest.fn() } } }
        ]
    });

    /**
     * The component attaches to a `tr`, so it needs a table to live in — see the
     * class doc for why an element wrapper is not an option.
     */
    const mount = () => {
        spectator = createHost(
            `<table><tbody>
                <tr dotKeyValueTableRow
                    [variable]="variable"
                    [index]="index"
                    [showHiddenField]="showHiddenField"></tr>
            </tbody></table>`,
            {
                hostProps: {
                    variable: mockVariable,
                    index: 0,
                    showHiddenField: false
                }
            }
        );
        spectator.detectChanges();

        return spectator;
    };

    /** TestBed can only be instantiated once per test, so props change in place. */
    const setProps = (props: Partial<Record<string, unknown>>) => {
        spectator.setHostInput(props);
        spectator.detectChanges();
    };

    beforeEach(() => mount());

    describe('at-rest presentation (FR-005, FR-008)', () => {
        it('should render the value as plain text, not an always-on input', () => {
            const output = spectator.query(byTestId('dot-key-value-value-output'));

            expect(output.textContent).toContain('John');
            expect(spectator.query(byTestId('dot-key-value-input'))).toBeFalsy();
        });

        it('should never expose an editable control for the key', () => {
            const keyCell = spectator.query(byTestId('dot-key-value-key'));

            expect(keyCell.textContent).toContain('name');
            expect(keyCell.querySelector('input')).toBeFalsy();
        });

        it('should render a stored "null" rather than dropping the pair', () => {
            setProps({ variable: { key: 'imported-key', hidden: false, value: 'null' } });

            expect(spectator.query(byTestId('dot-key-value-value-output')).textContent.trim()).toBe(
                'null'
            );
        });
    });

    describe('click-to-edit (FR-006, FR-007)', () => {
        const activate = () => {
            spectator.click(byTestId('dot-key-value-value-output'));
            spectator.detectChanges();
        };

        it('should turn the value into a focused input when activated', () => {
            activate();
            const input = spectator.query<HTMLInputElement>(byTestId('dot-key-value-input'));

            expect(input).toBeTruthy();
            expect(document.activeElement).toBe(input);
            expect(spectator.component.$isEditing()).toBe(true);
        });

        it('should commit the edit and return to plain text on Enter', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            const input = spectator.query<HTMLInputElement>(byTestId('dot-key-value-input'));
            spectator.typeInElement('edited', input);
            input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
            spectator.detectChanges();

            expect(saveSpy).toHaveBeenCalledWith({ ...mockVariable, value: 'edited' });
            expect(spectator.query(byTestId('dot-key-value-input'))).toBeFalsy();
        });

        it('should restore the original value on Escape and emit nothing', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            const input = spectator.query<HTMLInputElement>(byTestId('dot-key-value-input'));
            spectator.typeInElement('discard me', input);
            input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('dot-key-value-value-output')).textContent).toContain(
                'John'
            );
        });

        it('should not report an edit that changed nothing', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            spectator
                .query(byTestId('dot-key-value-input'))
                .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
        });
    });

    describe('removing', () => {
        it('should emit delete when the remove control is used', () => {
            const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');

            spectator.click(byTestId('dot-key-value-delete-button'));
            spectator.detectChanges();

            expect(deleteSpy).toHaveBeenCalled();
        });
    });

    describe('hover-revealed actions stay reachable (FR-017, FR-019)', () => {
        /**
         * Hiding an action with `display:none`, `[hidden]` or an `@if` on hover
         * state removes it from the tab order, which strands keyboard and touch
         * users with no visible symptom for anyone using a mouse. These tests
         * assert the mechanism, not merely that the icon "is not visible".
         */

        it.each([['dot-key-value-delete-button'], ['dot-key-value-drag-handle']])(
            'should keep %s in the DOM and out of sight via opacity only',
            (testId) => {
                const action = spectator.query(byTestId(testId));

                expect(action).toBeTruthy();
                expect(action.className).toContain('opacity-0');
                expect(action.className).not.toContain('hidden');

                const styles = getComputedStyle(action);
                expect(styles.display).not.toBe('none');
                expect(styles.visibility).not.toBe('hidden');
            }
        );

        it.each([['dot-key-value-delete-button'], ['dot-key-value-drag-handle']])(
            'should reveal %s on focus-within, not on hover alone',
            (testId) => {
                const action = spectator.query(byTestId(testId));

                expect(action.className).toContain('group-hover:opacity-100');
                expect(action.className).toContain('group-focus-within:opacity-100');
            }
        );

        it('should expose an accessible name on every action', () => {
            expect(
                spectator.query(byTestId('dot-key-value-delete-button')).getAttribute('aria-label')
            ).toBeTruthy();
            expect(
                spectator.query(byTestId('dot-key-value-drag-handle')).getAttribute('aria-label')
            ).toBeTruthy();
        });
    });

    describe('icons use Material Symbols (DC-003)', () => {
        it('should render the drag handle as the drag_indicator glyph', () => {
            const icon = spectator.query(byTestId('dot-key-value-drag-handle-icon'));

            expect(icon.className).toContain('material-symbols-outlined');
            expect(icon.textContent.trim()).toBe('drag_indicator');
        });

        it('should render the remove action as the close glyph', () => {
            const icon = spectator.query(byTestId('dot-key-value-delete-icon'));

            expect(icon.className).toContain('material-symbols-outlined');
            expect(icon.textContent.trim()).toBe('close');
        });

        it('should leave no PrimeIcons in the authored markup', () => {
            expect(spectator.element.querySelector('[class*="pi-"]')).toBeFalsy();
        });
    });

    describe('reordering', () => {
        it('should always render a drag handle — every consumer can reorder', () => {
            expect(spectator.query(byTestId('dot-key-value-drag-handle'))).toBeTruthy();
        });
    });

    describe('hidden values (FR-018, FR-021 to FR-025)', () => {
        const mountHidden = (hidden: boolean) =>
            setProps({ showHiddenField: true, variable: { ...mockVariable, hidden } });

        it('should state that a hidden value is withheld, under a lock', () => {
            mountHidden(true);

            expect(spectator.query(byTestId('dot-key-value-hidden-icon')).textContent.trim()).toBe(
                'lock'
            );
            expect(spectator.query(byTestId('dot-key-value-label'))).toBeTruthy();
            expect(spectator.element.textContent).not.toContain('John');
        });

        it('should keep the withheld state visible without hover or focus', () => {
            mountHidden(true);
            const label = spectator.query(byTestId('dot-key-value-label'));

            // State, not an action — so it must NOT carry the hover-reveal classes
            // the drag handle and remove control use.
            expect(label.className).not.toContain('opacity-0');
            expect(getComputedStyle(label).display).not.toBe('none');
        });

        it('should show a plain value as editable text', () => {
            mountHidden(false);

            expect(spectator.query(byTestId('dot-key-value-value-output')).textContent).toContain(
                'John'
            );
            expect(spectator.query(byTestId('dot-key-value-label'))).toBeFalsy();
        });

        it('should offer no in-place editing for a hidden value', () => {
            mountHidden(true);

            expect(spectator.query(byTestId('dot-key-value-value-output'))).toBeFalsy();
            expect(spectator.query(byTestId('dot-key-value-input'))).toBeFalsy();
        });

        it('should offer no visibility control on an existing row, hidden or not', () => {
            // Visibility is settled when the pair is created and never revisited. The
            // server sends `*****` in place of a stored secret, so a reveal would show
            // that mask, and saving it — or re-hiding a value already masked — would
            // overwrite the real secret with five asterisks.
            for (const hidden of [true, false]) {
                mountHidden(hidden);

                expect(spectator.query(byTestId('dot-key-value-visibility-toggle'))).toBeFalsy();
                expect(spectator.query(byTestId('dot-key-value-hidden-switch'))).toBeFalsy();
            }
        });

        it('should render no withheld state when the consumer opts out', () => {
            setProps({ showHiddenField: false, variable: { ...mockVariable, hidden: true } });

            expect(spectator.query(byTestId('dot-key-value-label'))).toBeFalsy();
            expect(spectator.query(byTestId('dot-key-value-value-output')).textContent).toContain(
                'John'
            );
        });
    });
});
