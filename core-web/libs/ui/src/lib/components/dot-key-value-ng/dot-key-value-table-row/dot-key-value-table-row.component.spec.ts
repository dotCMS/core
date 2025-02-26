/* eslint-disable @typescript-eslint/no-unused-vars */
import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { Table, TableModule, TableService } from 'primeng/table';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
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
    let spectatorHost: SpectatorHost<DotKeyValueTableRowComponent>;
    let dotMessageDisplayService: DotMessageDisplayService;
    const createHost = createHostFactory({
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
            MockProvider(DotMessageDisplayService),
            Table,
            TableService
        ]
    });

    beforeEach(() => {
        spectatorHost = createHost(
            `<dot-key-value-table-row
                [showHiddenField]="showHiddenField"
                [variable]="variable">
            </dot-key-value-table-row>`,
            {
                hostProps: {
                    showHiddenField: false,
                    variable: mockVariable
                }
            }
        );

        spectatorHost.detectChanges();
        dotMessageDisplayService = spectatorHost.inject(DotMessageDisplayService, true);
    });

    describe('Editable variables', () => {
        it('should load the component', () => {
            const deleteButton = spectatorHost.query(byTestId('dot-key-value-delete-button'));
            const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));
            const label = spectatorHost.query(byTestId('dot-editable-key-value'));

            expect(deleteButton).toBeTruthy();
            expect(editButton).toBeTruthy();
            expect(label).toBeTruthy();
        });

        it('should focus value cell when button is clicked', async () => {
            const valueCellSpy = jest.spyOn(
                spectatorHost.component.valueCell.nativeElement,
                'click'
            );
            const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));

            spectatorHost.click(editButton);
            await spectatorHost.fixture.whenStable();

            expect(valueCellSpy).toHaveBeenCalled();
        });

        it('should show edit menu when focus on a field', () => {
            const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));
            spectatorHost.click(editButton);
            spectatorHost.detectChanges();

            const valueInput = spectatorHost.query(
                byTestId('dot-key-value-input')
            ) as HTMLInputElement;
            valueInput.dispatchEvent(new FocusEvent('focus'));
            spectatorHost.detectChanges();

            const cancelButton = spectatorHost.query(byTestId('dot-key-value-cancel-button'));
            const saveButton = spectatorHost.query(
                byTestId('dot-key-value-save-button')
            ) as HTMLButtonElement;

            expect(cancelButton).toBeTruthy();
            expect(saveButton).toBeTruthy();
            expect(saveButton.disabled).toBeFalsy();
        });

        describe('Edit Input field is visible', () => {
            beforeEach(() => {
                const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));
                spectatorHost.click(editButton);
                spectatorHost.detectChanges();
            });

            it('should emit save event when button clicked', () => {
                const saveSpy = jest.spyOn(spectatorHost.component.save, 'emit');
                const valueInput = spectatorHost.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;

                valueInput.value = 'newValue';
                spectatorHost.dispatchFakeEvent(valueInput, 'input');
                spectatorHost.detectChanges();

                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectatorHost.detectChanges();

                const saveButton = spectatorHost.query(byTestId('dot-key-value-save-button'));

                expect(saveButton).toBeTruthy();
                spectatorHost.click(saveButton);

                expect(saveSpy).toHaveBeenCalledWith({
                    ...mockVariable,
                    value: 'newValue'
                });
            });

            it('should emit delete event when button clicked', () => {
                const deleteSpy = jest.spyOn(spectatorHost.component.delete, 'emit');
                const deleteButton = spectatorHost.query(byTestId('dot-key-value-delete-button'));

                spectatorHost.click(deleteButton);
                spectatorHost.detectChanges();

                expect(deleteSpy).toHaveBeenCalledWith(mockVariable);
            });

            it('should reset form when cancel is clicked', () => {
                const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));
                spectatorHost.click(editButton);
                spectatorHost.detectChanges();

                const valueInput = spectatorHost.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.value = 'newValue';
                spectatorHost.dispatchFakeEvent(valueInput, 'input');
                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectatorHost.detectChanges();

                const cancelButton = spectatorHost.query(byTestId('dot-key-value-cancel-button'));
                expect(cancelButton).toBeTruthy();
                spectatorHost.click(cancelButton);
            });

            it('should reset form when Escape key is pressed', () => {
                const valueInput = spectatorHost.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.value = 'newValue';
                spectatorHost.dispatchFakeEvent(valueInput, 'input');
                spectatorHost.detectChanges();

                valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
                spectatorHost.detectChanges();

                expect(spectatorHost.component.form.get('value').value).toBe(mockVariable.value);
            });
        });

        describe('Hidden Fields', () => {
            beforeEach(() => {
                spectatorHost.setHostInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: true }
                });
                spectatorHost.detectChanges();
            });

            it('should show the password placeholder instead of the value', () => {
                const valueLabel = spectatorHost.query(byTestId('dot-key-value-label'));
                expect(valueLabel.textContent.trim()).toBe(PASSWORD_PLACEHOLDER);
            });

            it('should have disabled edit controls when field is hidden', async () => {
                spectatorHost.setHostInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: true }
                });
                spectatorHost.detectChanges();
                await spectatorHost.fixture.whenStable();

                expect(spectatorHost.component.form).toBeTruthy();

                const inputSwitch = spectatorHost.query(byTestId('dot-key-value-hidden-switch'));
                const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));

                expect(inputSwitch).toBeTruthy();
                expect(editButton).toBeTruthy();
                expect(spectatorHost.component.isHiddenField).toBe(true);
                expect(editButton.getAttribute('ng-reflect-disabled')).toBe('true');
            });

            it('should toggle password visibility when switch is clicked', () => {
                spectatorHost.setHostInput({
                    showHiddenField: true,
                    variable: { ...mockVariable, hidden: false }
                });
                spectatorHost.detectChanges();

                const editButton = spectatorHost.query(byTestId('dot-key-value-edit-button'));
                spectatorHost.click(editButton);
                spectatorHost.detectChanges();

                // Trigger focus to show edit menu
                const valueInput = spectatorHost.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectatorHost.detectChanges();

                const inputSwitch = spectatorHost.query('p-inputSwitch');
                const switchElement = inputSwitch.querySelector('.p-inputswitch');
                spectatorHost.click(switchElement);
                spectatorHost.detectChanges();

                expect(spectatorHost.component.form.get('hidden').value).toBe(true);
                expect(spectatorHost.component.inputType).toBe('password');
            });
        });
    });
});
