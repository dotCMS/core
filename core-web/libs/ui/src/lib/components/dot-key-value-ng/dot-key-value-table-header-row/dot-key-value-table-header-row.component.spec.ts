import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableHeaderRowComponent } from './dot-key-value-table-header-row.component';

import { DotKeyValue } from '../dot-key-value-ng.component';

export const mockKeyValue: DotKeyValue[] = [
    {
        key: 'name',
        hidden: false,
        value: 'John'
    }
];

describe('DotKeyValueTableHeaderRowComponent', () => {
    let spectator: Spectator<DotKeyValueTableHeaderRowComponent>;
    const createComponent = createComponentFactory({
        component: DotKeyValueTableHeaderRowComponent,
        imports: [
            FormsModule,
            ReactiveFormsModule,
            InputSwitchModule,
            InputTextModule,
            ButtonModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'keyValue.key_input.placeholder': 'Enter Key',
                    'keyValue.value_input.placeholder': 'Enter Value',
                    'keyValue.key_input.required': 'Key is required',
                    'keyValue.value_input.required': 'Value is required',
                    'keyValue.key_input.duplicated': 'Key already exists',
                    'keyValue.key_header.label': 'Key',
                    'keyValue.value_header.label': 'Value',
                    'keyValue.hidden_header.label': 'Hidden',
                    Save: 'Save',
                    Cancel: 'Cancel',
                    'keyValue.error.duplicated.variable': 'test {0}'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                showHiddenField: false,
                forbiddenkeys: {
                    name: true
                },
                dragAndDrop: false
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should load the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Form Validation', () => {
        it('should invalidate form when key is empty', () => {
            spectator.component.keyControl.setValue('');
            spectator.component.keyControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.form.valid).toBeFalsy();
            expect(spectator.component.keyControl.hasError('required')).toBeTruthy();
            expect(spectator.query('.error-message')).toHaveText('Key is required');
        });

        it('should invalidate form when key is forbidden', () => {
            spectator.component.keyControl.setValue('name');
            spectator.component.keyControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.form.valid).toBeFalsy();
            expect(spectator.component.keyControl.hasError('duplicatedKey')).toBeTruthy();
            expect(spectator.query('.error-message')).toHaveText('Key already exists');
        });

        it('should invalidate form when value is empty', () => {
            spectator.component.valueControl.setValue('');
            spectator.component.valueControl.markAsDirty();
            spectator.detectChanges();

            expect(spectator.component.form.valid).toBeFalsy();
            expect(spectator.component.valueControl.hasError('required')).toBeTruthy();
            expect(spectator.query('.error-message')).toHaveText('Value is required');
        });
    });

    describe('Save Functionality', () => {
        it('should emit save event with form values when form is valid', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            const resetSpy = jest.spyOn(spectator.component, 'resetForm');

            spectator.component.keyControl.setValue('newKey');
            spectator.component.valueControl.setValue('newValue');
            spectator.component.hiddenControl.setValue(true);

            spectator.component.saveVariable();

            expect(saveSpy).toHaveBeenCalledWith({
                key: 'newKey',
                value: 'newValue',
                hidden: true
            });
            expect(resetSpy).toHaveBeenCalled();
        });

        it('should not emit save event when form is invalid', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            const resetSpy = jest.spyOn(spectator.component, 'resetForm');

            // Leave form invalid
            spectator.component.keyControl.setValue('');

            spectator.component.saveVariable();

            expect(saveSpy).not.toHaveBeenCalled();
            expect(resetSpy).not.toHaveBeenCalled();
            expect(spectator.component.keyControl.touched).toBeTruthy();
            expect(spectator.component.valueControl.touched).toBeTruthy();
        });
    });

    describe('Key Input Events', () => {
        it('should focus on "Value" field, if entered a valid "Key"', () => {
            const keyInput = spectator.query<HTMLInputElement>(byTestId('key-input'));
            spectator.typeInElement('valid-key', keyInput);
            expect(spectator.component.keyControl.value).toBe('valid-key');
            keyInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            const valueInputElement = spectator.component.$valueCell().nativeElement;
            expect(document.activeElement).toEqual(valueInputElement);
        });

        it('should stay on key input when key is invalid and Enter is pressed', () => {
            const keyInput = spectator.query<HTMLInputElement>(byTestId('key-input'));
            spectator.typeInElement('name', keyInput); // This is a forbidden key
            keyInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            const keyInputElement = spectator.component.$keyCell().nativeElement;
            expect(document.activeElement).toEqual(keyInputElement);
        });
    });

    describe('Value Input Events', () => {
        it('should call saveVariable when Enter is pressed in value input', () => {
            const saveVariableSpy = jest.spyOn(spectator.component, 'saveVariable');
            const valueInput = spectator.query<HTMLInputElement>(byTestId('value-input'));
            valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            expect(saveVariableSpy).toHaveBeenCalled();
        });

        it('should reset form when press "Escape"', () => {
            const spyForm = jest.spyOn(spectator.component.form, 'reset');
            const valueInput = spectator.query<HTMLInputElement>(byTestId('value-input'));
            valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            expect(spyForm).toHaveBeenCalled();
        });
    });

    describe('Cancel Functionality', () => {
        it('should reset form when onCancel is called', () => {
            const resetSpy = jest.spyOn(spectator.component, 'resetForm');
            const mockEvent = new Event('click');
            const stopPropagationSpy = jest.spyOn(mockEvent, 'stopPropagation');

            spectator.component.onCancel(mockEvent);

            expect(stopPropagationSpy).toHaveBeenCalled();
            expect(resetSpy).toHaveBeenCalled();
        });
    });

    describe('Drag and Drop', () => {
        it('should not show drag handle column when dragAndDrop is false', () => {
            spectator.setInput('dragAndDrop', false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('drag-column'))).toBeNull();
        });

        it('should show drag handle column when dragAndDrop is true', () => {
            spectator.setInput('dragAndDrop', true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('drag-column'))).toBeTruthy();
        });
    });

    describe('With Hidden Fields', () => {
        beforeEach(() => {
            spectator.setInput('showHiddenField', true);
        });

        it('should load the component with switch button', () => {
            spectator.detectChanges();
            const switchInput = spectator.query(InputSwitch);
            expect(switchInput).toBeTruthy();
        });

        it('should switch to hidden mode when clicked on the hidden switch button', async () => {
            spectator.detectChanges();
            const switchInput = spectator.query(byTestId('hidden-switch')).querySelector('input');

            spectator.click(switchInput);
            spectator.detectChanges();

            const valueInput = spectator.query<HTMLInputElement>(byTestId('value-input'));
            expect(valueInput.type).toBe('password');
        });
    });
});
