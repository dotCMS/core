import { SpectatorHost, byTestId, createHostFactory } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableHeaderRowComponent } from './dot-key-value-table-header-row.component';

const messageServiceMock = new MockDotMessageService({
    'keyValue.key_input.placeholder': 'Enter Key',
    'keyValue.value_input.placeholder': 'Enter Value',
    'keyValue.key_input.required': 'This field is required',
    'keyValue.value_input.required': 'This field is required',
    'keyValue.key_input.duplicated': 'This key already exists',
    'keyValue.hidden_header.label': 'Hidden',
    add: 'Add'
});

describe('DotKeyValueTableHeaderRowComponent', () => {
    let spectator: SpectatorHost<DotKeyValueTableHeaderRowComponent>;

    const createHost = createHostFactory({
        component: DotKeyValueTableHeaderRowComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    /**
     * The component attaches to a `tr`, so it needs a table to live in — see the
     * class doc for why an element wrapper is not an option.
     */
    const HOST = `
        <table><thead>
            <tr dotKeyValueTableHeaderRow
                [forbiddenkeys]="forbiddenkeys"
                [showHiddenField]="showHiddenField"></tr>
        </thead></table>`;

    /** TestBed can only be instantiated once per test, so props change in place. */
    const setProps = (props: Partial<Record<string, unknown>>) => {
        spectator.setHostInput(props);
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createHost(HOST, {
            hostProps: {
                forbiddenkeys: { name: true },
                showHiddenField: false
            }
        });
        spectator.detectChanges();
    });

    const fill = (key: string, value: string) => {
        spectator.typeInElement(key, spectator.query(byTestId('key-input')));
        spectator.typeInElement(value, spectator.query(byTestId('value-input')));
        spectator.detectChanges();
    };

    describe('validation (FR-010, FR-011)', () => {
        it('should reject an empty key and say why', () => {
            spectator.component.keyControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.form.valid).toBe(false);
            expect(spectator.query('small.text-red-500').textContent).toContain(
                'This field is required'
            );
        });

        it('should reject a duplicate key and say why', () => {
            spectator.component.keyControl.setValue('name');
            spectator.component.keyControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.keyControl.hasError('duplicatedKey')).toBe(true);
            expect(spectator.query('small.text-red-500').textContent).toContain(
                'This key already exists'
            );
        });

        it('should clear the duplicate error once the conflicting key is gone', () => {
            spectator.component.keyControl.setValue('name');
            spectator.component.keyControl.markAsDirty();
            spectator.detectChanges();
            expect(spectator.component.keyControl.hasError('duplicatedKey')).toBe(true);

            setProps({ forbiddenkeys: {} });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.component.keyControl.errors).toBeNull();
        });

        it('should reject an empty value', () => {
            spectator.component.keyControl.setValue('k');
            spectator.component.valueControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.form.valid).toBe(false);
        });

        it('should not emit when the form is invalid', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.click(byTestId('save-button'));
            spectator.detectChanges();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(spectator.component.keyControl.touched).toBe(true);
            expect(spectator.component.valueControl.touched).toBe(true);
        });
    });

    describe('adding a pair', () => {
        it('should emit the completed pair', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            fill('newKey', 'newValue');
            spectator.click(byTestId('save-button'));
            spectator.detectChanges();

            expect(saveSpy).toHaveBeenCalledWith({
                key: 'newKey',
                value: 'newValue',
                hidden: false
            });
        });

        it('should emit on Enter from the value input', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            fill('fromKeyboard', 'value');
            spectator
                .query(byTestId('value-input'))
                .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
            spectator.detectChanges();

            expect(saveSpy).toHaveBeenCalled();
        });

        // FR-012: consecutive pairs must be enterable without reaching for the pointer.
        it('should clear the form and refocus the key input after a successful add', () => {
            const keyInput = spectator.query<HTMLInputElement>(byTestId('key-input'));

            fill('newKey', 'newValue');
            spectator.click(byTestId('save-button'));
            spectator.detectChanges();

            expect(spectator.component.keyControl.value).toBe('');
            expect(spectator.component.valueControl.value).toBe('');
            expect(document.activeElement).toBe(keyInput);
        });
    });

    describe('keyboard navigation', () => {
        it('should advance from key to value on Enter when the key is usable', () => {
            const keyInput = spectator.query<HTMLInputElement>(byTestId('key-input'));
            spectator.typeInElement('valid-key', keyInput);
            keyInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            expect(document.activeElement).toBe(spectator.component.$valueCell().nativeElement);
        });

        it('should stay on the key input when the key is invalid', () => {
            const keyInput = spectator.query<HTMLInputElement>(byTestId('key-input'));
            spectator.typeInElement('name', keyInput); // duplicate
            keyInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            expect(document.activeElement).toBe(keyInput);
        });

        it('should reset the row on Escape', () => {
            fill('abandoned', 'value');
            spectator
                .query(byTestId('key-input'))
                .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            spectator.detectChanges();

            expect(spectator.component.keyControl.value).toBe('');
        });
    });

    describe('hidden values (FR-021)', () => {
        it('should render no visibility toggle when the consumer opts out', () => {
            expect(spectator.query(byTestId('dot-key-value-new-visibility-toggle'))).toBeFalsy();
        });

        it('should offer the toggle inside the value field, not a column of its own', () => {
            setProps({ showHiddenField: true });

            expect(spectator.query(byTestId('dot-key-value-new-visibility-toggle'))).toBeTruthy();
            expect(spectator.query(byTestId('hidden-switch'))).toBeFalsy();
        });

        it('should switch the value field to a password when the toggle is used', () => {
            setProps({ showHiddenField: true });

            spectator.click(byTestId('dot-key-value-new-visibility-toggle'));
            spectator.detectChanges();

            expect(spectator.query<HTMLInputElement>(byTestId('value-input')).type).toBe(
                'password'
            );
        });
    });

    describe('column alignment', () => {
        it('should always render the leading cell, so it lines up with the drag column', () => {
            expect(spectator.query(byTestId('drag-column'))).toBeTruthy();
        });
    });
});
