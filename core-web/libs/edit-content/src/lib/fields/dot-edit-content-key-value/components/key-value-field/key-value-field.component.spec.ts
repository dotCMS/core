import { Spectator, createComponentFactory } from '@openng/spectator/jest';

import { DotKeyValueComponent } from '@dotcms/ui';

import { DotKeyValueFieldComponent } from './key-value-field.component';

/**
 * Guards the Edit Content consumer's data round-trip (FR-033).
 *
 * This component sits between the shared editor's `DotKeyValue[]` and the
 * contentlet's `Record<string, string>`. The redesign must not disturb that
 * mapping in either direction — including the "null" value that legacy imports
 * leave behind.
 */
describe('DotKeyValueFieldComponent', () => {
    let spectator: Spectator<DotKeyValueFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotKeyValueFieldComponent,
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent({ props: { hasError: false } as unknown });
    });

    describe('reading stored values', () => {
        it('should map a stored record into pairs preserving insertion order', () => {
            spectator.component.writeValue({ zeta: 'last', alpha: 'first' });
            spectator.detectChanges();

            expect(spectator.component.$initialValue()).toEqual([
                { key: 'zeta', value: 'last' },
                { key: 'alpha', value: 'first' }
            ]);
        });

        it('should render a stored null as the literal "null" rather than dropping the pair', () => {
            spectator.component.writeValue({ imported: null });
            spectator.detectChanges();

            expect(spectator.component.$initialValue()).toEqual([
                { key: 'imported', value: 'null' }
            ]);
        });

        it.each([[null], [undefined], ['not an object'], [[]]])(
            'should fall back to an empty list for the malformed value %p',
            (value) => {
                spectator.component.writeValue(value as never);
                spectator.detectChanges();

                expect(spectator.component.$initialValue()).toEqual([]);
            }
        );
    });

    describe('writing values back', () => {
        it('should map pairs back into a record in the order given', () => {
            const onChange = jest.fn();
            spectator.component.registerOnChange(onChange);

            const pairs = [
                { key: 'zeta', value: 'last' },
                { key: 'alpha', value: 'first' }
            ];
            spectator.component.updateField(pairs);

            expect(Object.keys(onChange.mock.calls[0][0])).toEqual(['zeta', 'alpha']);
            expect(onChange).toHaveBeenCalledWith({ zeta: 'last', alpha: 'first' });
        });

        it('should mark the control touched when the list changes', () => {
            const onTouched = jest.fn();
            spectator.component.registerOnTouched(onTouched);

            spectator.component.updateField([{ key: 'a', value: '1' }]);

            expect(onTouched).toHaveBeenCalled();
        });

        it('should round-trip a populated field without loss', () => {
            const stored = { analyticsId: 'UA-4419-22', theme: 'dark' };
            const onChange = jest.fn();

            spectator.component.writeValue(stored);
            spectator.detectChanges(); // `handleChangeValue` is a signalMethod — it runs in an effect
            spectator.component.registerOnChange(onChange);
            spectator.component.updateField(spectator.component.$initialValue());

            expect(onChange).toHaveBeenCalledWith(stored);
        });
    });

    describe('shared editor wiring (FR-030)', () => {
        it('should not offer hidden values in Edit Content', () => {
            const editor = spectator.query(DotKeyValueComponent);

            expect(editor.$showHiddenField()).toBe(false);
        });
    });
});
