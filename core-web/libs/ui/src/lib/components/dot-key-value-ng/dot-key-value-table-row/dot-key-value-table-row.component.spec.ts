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
    'keyValue.value_input.required': 'This field is required',
    'keyValue.key_input.required': 'This field is required',
    'keyValue.key_input.duplicated': 'This key already exists',
    'keyValue.action.delete': 'Delete'
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
                    [forbiddenkeys]="forbiddenkeys"
                    [showHiddenField]="showHiddenField"></tr>
            </tbody></table>`,
            {
                hostProps: {
                    variable: mockVariable,
                    index: 0,
                    forbiddenkeys: {},
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

        it('should never shift a column vertically, opening or refusing', () => {
            /*
             * At rest the row keeps the browser default of `middle`, so nothing moves
             * while it is only being read.
             *
             * The moment a cell opens, every cell top-aligns instead — and that is what
             * holds the two columns level. A refusal renders its message inside the cell
             * but below the band, making that cell taller than its sibling; left at
             * `middle` the sibling would re-centre against the taller row and sit about
             * 10px lower than the cell being edited. Anchoring both bands to the top
             * keeps their centres at the same height however far the row grows.
             */
            const alignment = () => [
                spectator.query(byTestId('dot-key-value-key')).className,
                spectator.query(byTestId('dot-key-value-editable-column')).className
            ];

            expect(alignment().join()).not.toContain('align-top');

            activate();
            expect(alignment().every((className) => className.includes('align-top'))).toBe(true);

            // Refused: the message makes this cell the taller of the two.
            spectator.component.editControl.setValue('   ');
            spectator.component.commitEdit();
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-key-value-value-required'))).toBeTruthy();
            expect(alignment().every((className) => className.includes('align-top'))).toBe(true);
        });

        it('should discard the edit when focus leaves the input', () => {
            // Enter is the one gesture that writes. Clicking away abandons, so a valid
            // edit and an invalid one leave by the same door.
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            const input = spectator.query<HTMLInputElement>(byTestId('dot-key-value-input'));
            spectator.typeInElement('edited', input);
            spectator.dispatchFakeEvent(input, 'blur');
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('dot-key-value-value-output')).textContent.trim()).toBe(
                mockVariable.value
            );
        });

        it('should refuse an emptied value and say why', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            spectator.component.editControl.setValue('   ');
            spectator.component.commitEdit();
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('dot-key-value-input'))).toBeTruthy();
            expect(spectator.query(byTestId('dot-key-value-value-required'))).toBeTruthy();
        });

        it('should commit a value exactly as typed, spaces included', () => {
            // Trimming decides only whether a value counts as blank; it never edits it.
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            activate();

            spectator.component.editControl.setValue('John ');
            spectator.component.commitEdit();

            expect(saveSpy).toHaveBeenCalledWith({ ...mockVariable, value: 'John ' });
        });

        it('should activate the value with Space as well as Enter', () => {
            const output = spectator.query(byTestId('dot-key-value-value-output'));
            const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });

            output.dispatchEvent(event);
            spectator.detectChanges();

            expect(spectator.component.$isEditing()).toBe(true);
            expect(event.defaultPrevented).toBe(true);
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

        it('should expose an accessible name on the actions a keyboard can reach', () => {
            expect(
                spectator.query(byTestId('dot-key-value-delete-button')).getAttribute('aria-label')
            ).toBeTruthy();
        });

        it('should not present the drag handle as an operable control', () => {
            // PrimeNG's row reorder is pointer-driven: it binds mousedown and the HTML
            // drag events, and nothing for Enter, Space or the arrows. A role and a tab
            // stop here would announce a button that cannot be operated at all.
            const handle = spectator.query(byTestId('dot-key-value-drag-handle'));

            expect(handle).toBeTruthy();
            expect(handle.getAttribute('role')).toBeNull();
            expect(handle.getAttribute('tabindex')).toBeNull();
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

    describe('editing the key', () => {
        const startEditing = () => {
            spectator.click(byTestId('dot-key-value-key-output'));
            spectator.detectChanges();
        };

        it('should swap the key for an input carrying its current text', () => {
            startEditing();
            const input = spectator.query(byTestId('dot-key-value-key-input'));

            expect(input).toBeTruthy();
            expect(spectator.component.editControl.value).toBe(mockVariable.key);
            expect(spectator.query(byTestId('dot-key-value-key-output'))).toBeFalsy();
        });

        it('should report the renamed pair, keeping its value', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            startEditing();

            spectator.component.editControl.setValue('renamed');
            spectator.component.commitEdit();

            expect(saveSpy).toHaveBeenCalledWith({
                ...mockVariable,
                key: 'renamed'
            });
        });

        it('should say nothing when the key comes back unchanged', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            startEditing();

            spectator.component.commitEdit();

            expect(saveSpy).not.toHaveBeenCalled();
        });

        it('should restore the key on Escape', () => {
            startEditing();
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.component.editControl.setValue('typed-but-discarded');
            spectator.component.cancelEdit();
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('dot-key-value-key-output')).textContent.trim()).toBe(
                mockVariable.key
            );
        });

        it('should refuse a rename onto a key another row holds', () => {
            // Two rows sharing a key collide wherever the pairs are stored, and the
            // entry row already rejects the same thing when adding.
            setProps({ variable: mockVariable, forbiddenkeys: { taken: true } });
            startEditing();
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.component.editControl.setValue('taken');
            spectator.component.commitEdit();

            expect(saveSpy).not.toHaveBeenCalled();
        });

        it('should refuse an empty key', () => {
            startEditing();
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.component.editControl.setValue('   ');
            spectator.component.commitEdit();

            expect(saveSpy).not.toHaveBeenCalled();
        });

        it('should not report the row as a duplicate of itself', () => {
            // The row's own key is in the forbidden map, so the validator has to exclude
            // it — otherwise the row would flag its own key the moment editing opened.
            setProps({
                variable: { key: 'name', value: 'John' },
                forbiddenkeys: { name: true, taken: true }
            });
            startEditing();

            expect(spectator.component.editControl.valid).toBe(true);

            spectator.component.editControl.setValue('taken');
            expect(spectator.component.editControl.hasError('duplicatedKey')).toBe(true);

            // Back to its own key, whitespace and all: still not a collision.
            spectator.component.editControl.setValue('  name  ');
            expect(spectator.component.editControl.valid).toBe(true);
        });

        it('should say nothing when only whitespace was trimmed off the key', () => {
            setProps({
                variable: { key: 'name', value: 'John' },
                forbiddenkeys: { name: true }
            });
            startEditing();
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.component.editControl.setValue('  name  ');
            spectator.component.commitEdit();

            // Trimmed back to the same key: nothing changed, so nothing is reported.
            expect(saveSpy).not.toHaveBeenCalled();
        });

        it('should keep the input open and say why a rename was refused', () => {
            setProps({ variable: mockVariable, forbiddenkeys: { taken: true } });
            startEditing();

            spectator.component.editControl.setValue('taken');
            spectator.component.commitEdit();
            spectator.detectChanges();

            // Closing here would discard what was typed without ever saying why.
            expect(spectator.query(byTestId('dot-key-value-key-input'))).toBeTruthy();
            expect(spectator.component.editControl.value).toBe('taken');
            expect(spectator.query(byTestId('dot-key-value-key-duplicated'))).toBeTruthy();
        });

        it('should discard a rename when focus leaves the input', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            startEditing();

            const input = spectator.query<HTMLInputElement>(byTestId('dot-key-value-key-input'));
            spectator.typeInElement('renamed', input);
            spectator.dispatchFakeEvent(input, 'blur');
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('dot-key-value-key-output')).textContent.trim()).toBe(
                mockVariable.key
            );
        });

        it('should keep the input open and say why an empty key was refused', () => {
            startEditing();

            spectator.component.editControl.setValue('   ');
            spectator.component.commitEdit();
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-key-value-key-input'))).toBeTruthy();
            expect(spectator.query(byTestId('dot-key-value-key-required'))).toBeTruthy();
        });

        it('should activate the key with Space as well as Enter', () => {
            const output = spectator.query(byTestId('dot-key-value-key-output'));
            const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });

            output.dispatchEvent(event);
            spectator.detectChanges();

            expect(spectator.component.$isEditingKey()).toBe(true);
            // Space scrolls the page on anything that is not a real button.
            expect(event.defaultPrevented).toBe(true);
        });

        it('should edit the key and the value independently', () => {
            startEditing();
            expect(spectator.query(byTestId('dot-key-value-value-output'))).toBeTruthy();

            spectator.component.cancelEdit();
            spectator.detectChanges();
            spectator.click(byTestId('dot-key-value-value-output'));
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-key-value-key-output'))).toBeTruthy();
            expect(spectator.query(byTestId('dot-key-value-input'))).toBeTruthy();
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
