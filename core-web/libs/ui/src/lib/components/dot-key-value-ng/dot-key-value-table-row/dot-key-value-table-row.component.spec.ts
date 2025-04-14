import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { Table, TableModule, TableService } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

const mockVariable: DotKeyValue = {
    key: 'name',
    hidden: false,
    value: 'John'
};

const PASSWORD_PLACEHOLDER = '*****';

describe('DotKeyValueTableRowComponent', () => {
    let spectator: Spectator<DotKeyValueTableRowComponent>;
    const createComponent = createComponentFactory({
        component: DotKeyValueTableRowComponent,
        imports: [
            FormsModule,
            ReactiveFormsModule,
            InputSwitchModule,
            InputTextModule,
            ButtonModule,
            InputTextareaModule,
            TableModule,
            DotMessagePipe,
            NoopAnimationsModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'keyValue.key_input.placeholder': 'Enter Key',
                    'keyValue.value_input.placeholder': 'Enter Value',
                    Save: 'Save',
                    Cancel: 'Cancel',
                    'keyValue.error.duplicated.variable': 'test {0}'
                })
            },
            Table,
            TableService
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                showHiddenField: false,
                variable: mockVariable
            } as unknown
        });

        spectator.detectChanges();
    });

    describe('Editable variables', () => {
        it('should load the component', () => {
            const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));
            const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
            const label = spectator.query(byTestId('dot-editable-key-value'));

            expect(deleteButton).toBeTruthy();
            expect(editButton).toBeTruthy();
            expect(label).toBeTruthy();
        });

        it('should show edit menu when focus on a field', () => {
            const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
            spectator.click(editButton);
            spectator.detectChanges();

            const valueInput = spectator.query(byTestId('dot-key-value-input')) as HTMLInputElement;
            valueInput.dispatchEvent(new FocusEvent('focus'));
            spectator.detectChanges();

            const cancelButton = spectator.query(byTestId('dot-key-value-cancel-button'));
            const saveButton = spectator.query(
                byTestId('dot-key-value-save-button')
            ) as HTMLButtonElement;

            expect(cancelButton).toBeTruthy();
            expect(saveButton).toBeTruthy();
            expect(saveButton.disabled).toBeFalsy();
        });

        describe('Edit Input field is visible', () => {
            beforeEach(() => {
                const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
                spectator.click(editButton);
                spectator.detectChanges();
            });

            it('should emit save event when button clicked', () => {
                const saveSpy = jest.spyOn(spectator.component.save, 'emit');
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;

                valueInput.value = 'newValue';
                spectator.dispatchFakeEvent(valueInput, 'input');
                spectator.detectChanges();

                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectator.detectChanges();

                const saveButton = spectator.query(byTestId('dot-key-value-save-button'));

                expect(saveButton).toBeTruthy();
                spectator.click(saveButton);

                expect(saveSpy).toHaveBeenCalledWith({
                    ...mockVariable,
                    value: 'newValue'
                });
            });

            it('should emit delete event when button clicked', () => {
                const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');
                const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));

                spectator.click(deleteButton);
                spectator.detectChanges();

                expect(deleteSpy).toHaveBeenCalledWith(mockVariable);
            });

            it('should reset form when cancel is clicked', () => {
                const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
                spectator.click(editButton);
                spectator.detectChanges();

                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.value = 'newValue';
                spectator.dispatchFakeEvent(valueInput, 'input');
                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectator.detectChanges();

                const cancelButton = spectator.query(byTestId('dot-key-value-cancel-button'));
                expect(cancelButton).toBeTruthy();
                spectator.click(cancelButton);
            });

            it('should reset form when Escape key is pressed', () => {
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.value = 'newValue';
                spectator.dispatchFakeEvent(valueInput, 'input');
                spectator.detectChanges();

                valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
                spectator.detectChanges();

                expect(spectator.component.form.get('value').value).toBe(mockVariable.value);
            });
        });

        describe('Hidden Fields', () => {
            beforeEach(() => {
                spectator.setInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: true }
                });
                spectator.detectChanges();
            });

            it('should show the password placeholder instead of the value', () => {
                const valueLabel = spectator.query(byTestId('dot-key-value-label'));
                expect(valueLabel.textContent.trim()).toBe(PASSWORD_PLACEHOLDER);
            });

            it('should have disabled edit controls when field is hidden', async () => {
                spectator.setInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: true }
                });
                spectator.detectChanges();
                await spectator.fixture.whenStable();

                expect(spectator.component.form).toBeTruthy();

                const inputSwitch = spectator.query(byTestId('dot-key-value-hidden-switch'));
                const editButton = spectator.query(byTestId('dot-key-value-edit-button'));

                expect(inputSwitch).toBeTruthy();
                expect(editButton).toBeTruthy();
                expect(spectator.component.$isHiddenField()).toBe(true);
                expect(editButton.getAttribute('ng-reflect-disabled')).toBe('true');
            });

            it('should toggle password visibility when switch is clicked', () => {
                spectator.setInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: false }
                });
                spectator.detectChanges();

                const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
                spectator.click(editButton);
                spectator.detectChanges();

                // Trigger focus to show edit menu
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectator.detectChanges();

                const inputSwitch = spectator.query('p-inputSwitch');
                const switchElement = inputSwitch.querySelector('.p-inputswitch');
                spectator.click(switchElement);
                spectator.detectChanges();

                expect(spectator.component.form.get('hidden').value).toBe(true);
                expect(spectator.component.inputType).toBe('password');
            });
        });
    });
});
