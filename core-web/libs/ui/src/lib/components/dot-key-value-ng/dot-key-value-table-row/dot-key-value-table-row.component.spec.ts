import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { Component, Input, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

export const mockKeyValue: DotKeyValue[] = [
    {
        key: 'user',
        hidden: false,
        value: 'Ben'
    },
    {
        key: 'admin',
        hidden: true,
        value: 'admin'
    }
];

const PASSWORD_PLACEHOLDER = '*****';

const MESSAGE_MOCK_SERVICE = new MockDotMessageService({
    'keyValue.key_input.placeholder': 'Enter Key',
    'keyValue.value_input.placeholder': 'Enter Value',
    Save: 'Save',
    Cancel: 'Cancel',
    'keyValue.error.duplicated.variable': 'test {0}'
});

const triggerEditButtonClick = (spectator: Spectator<TestHostComponent>) => {
    const button = spectator.query(byTestId('dot-key-value-edit-button'));
    spectator.click(button);
};

const triggerEditableColumnClick = (spectator: Spectator<TestHostComponent>) => {
    const editableColumn = spectator.query(byTestId('dot-key-value-editable-column'));
    spectator.click(editableColumn);
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <p-table [value]="variablesList" #table>
            <ng-template pTemplate="body" let-variable let-rowIndex="rowIndex">
                <dot-key-value-table-row
                    [showHiddenField]="showHiddenField"
                    [variable]="variable"></dot-key-value-table-row>
            </ng-template>
        </p-table>
    `
})
class TestHostComponent {
    @ViewChild(DotKeyValueTableRowComponent) row: DotKeyValueTableRowComponent;

    @Input() showHiddenField: boolean;
    @Input() variablesList: DotKeyValue[];
}

describe('DotKeyValueTableRowComponent', () => {
    let spectator: Spectator<TestHostComponent>;
    let dotKeyValueTableRowComponent: DotKeyValueTableRowComponent;

    const createHost = createComponentFactory({
        component: TestHostComponent,
        imports: [
            CommonModule,
            ButtonModule,
            InputSwitchModule,
            InputTextareaModule,
            InputTextModule,
            FormsModule,
            ReactiveFormsModule,
            TableModule,
            DotMessagePipe,
            NoopAnimationsModule,
            DotKeyValueTableRowComponent
        ],
        providers: [{ provide: DotMessageService, useValue: MESSAGE_MOCK_SERVICE }]
    });

    beforeEach(() => {
        spectator = createHost({
            props: {
                showHiddenField: false,
                variablesList: [mockKeyValue[0]]
            },
            detectChanges: false
        });
        spectator.detectChanges();
        dotKeyValueTableRowComponent = spectator.component.row;
    });

    describe('Editable variables', () => {
        it('should load the component', () => {
            const cellEditor = spectator.query('p-cellEditor');
            const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));
            const editButton = spectator.query(byTestId('dot-key-value-edit-button'));
            const label = spectator.query(byTestId('dot-editable-key-value'));

            expect(cellEditor).toBeTruthy();
            expect(deleteButton).toBeTruthy();
            expect(editButton).toBeTruthy();
            expect(label).toBeTruthy();
        });

        it('should focus value cell when button is clicked', () => {
            const valueCellSpy = spyOn(
                dotKeyValueTableRowComponent.valueCell.nativeElement,
                'click'
            );
            triggerEditButtonClick(spectator);

            spectator.detectChanges();

            expect(valueCellSpy).toHaveBeenCalled();
        });

        it('should show edit menu when focus on a field', () => {
            expect(dotKeyValueTableRowComponent.showHiddenField).toBeFalsy();
            expect(dotKeyValueTableRowComponent.variable).toEqual(mockKeyValue[0]);

            triggerEditButtonClick(spectator);

            spectator.detectChanges();

            const valueInput = spectator.query(byTestId('dot-key-value-input')) as HTMLInputElement;
            expect(valueInput).toBeTruthy();

            valueInput.dispatchEvent(new FocusEvent('focus'));
            spectator.detectChanges();

            const cancelButton = spectator.query(
                byTestId('dot-key-value-cancel-button')
            ) as HTMLButtonElement;
            const saveButton = spectator.query(
                byTestId('dot-key-value-save-button')
            ) as HTMLButtonElement;

            expect(cancelButton).toBeTruthy();
            expect(saveButton).toBeTruthy();
            expect(saveButton.disabled).toBeFalsy();
        });

        describe('showHiddenField is true', () => {
            beforeEach(() => {
                spectator.setInput('showHiddenField', true);
                spectator.detectChanges();
            });

            it('should switch to hidden mode when clicked on the hidden switch button', () => {
                const inputSwitch = spectator.query(InputSwitch);
                const inputSwitchElement = spectator.query('.p-inputswitch') as HTMLElement;

                expect(inputSwitch.checked()).toBeFalsy();
                spectator.click(inputSwitchElement);

                spectator.detectChanges();
                triggerEditableColumnClick(spectator);

                spectator.detectChanges();

                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                expect(valueInput).toBeTruthy();

                expect(valueInput.type).toBe('password');
                expect(inputSwitch.checked()).toBeTruthy();
            });
        });

        describe('Edit Input field is visible', () => {
            beforeEach(() => {
                spectator.setInput('showHiddenField', true);
                triggerEditButtonClick(spectator);
                spectator.detectChanges();
            });

            it('should reset to default values when press "Cancel Button is clicked"', () => {
                const inputSwitch = spectator.query(InputSwitch);
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;
                const inputSwitchElement = spectator.query('.p-inputswitch') as HTMLElement;
                const form = dotKeyValueTableRowComponent.form;

                valueInput.value = 'newKey';
                spectator.dispatchFakeEvent(valueInput, 'input');
                spectator.click(inputSwitchElement);

                expect(form.get('value').value).toBe('newKey');
                expect(inputSwitch.checked()).toBeTruthy();

                spectator.detectChanges();

                const cancelButton = spectator.query(
                    byTestId('dot-key-value-cancel-button')
                ) as HTMLButtonElement;
                spectator.click(cancelButton);

                spectator.detectChanges();

                expect(form.get('value').value).toBe(mockKeyValue[0].value);
                expect(inputSwitch.checked()).toBeFalsy();
            });

            it('should reset to default values when press "Escape"', () => {
                const form = dotKeyValueTableRowComponent.form;
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;

                valueInput.value = 'newKey';
                spectator.dispatchFakeEvent(valueInput, 'input');
                spectator.detectChanges();

                expect(form.get('value').value).toBe('newKey');

                valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
                spectator.detectChanges();

                expect(form.get('value').value).toBe(mockKeyValue[0].value);
            });

            it('should emit save event when button clicked', () => {
                const saveSpy = spyOn(dotKeyValueTableRowComponent.save, 'emit');
                const valueInput = spectator.query(
                    byTestId('dot-key-value-input')
                ) as HTMLInputElement;

                valueInput.value = 'newKey';
                valueInput.dispatchEvent(new FocusEvent('focus'));
                spectator.dispatchFakeEvent(valueInput, 'input');
                spectator.detectChanges();

                // click on save button
                const saveButton = spectator.query(byTestId('dot-key-value-save-button'));

                spectator.click(saveButton);
                spectator.detectChanges();

                expect(saveSpy).toHaveBeenCalledWith({
                    ...mockKeyValue[0],
                    value: 'newKey'
                });
            });

            it('should emit delete event when button clicked', () => {
                const deleteSpy = spyOn(dotKeyValueTableRowComponent.delete, 'emit');
                const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));

                spectator.click(deleteButton);
                spectator.detectChanges();

                expect(deleteSpy).toHaveBeenCalledWith(mockKeyValue[0]);
            });
        });

        describe('With Hidden Fields', () => {
            beforeEach(() => {
                spectator.setInput('showHiddenField', true);
                spectator.setInput('variablesList', [mockKeyValue[1]]);
                spectator.detectChanges();
            });

            it('should show the password placeholder instead of the key value', () => {
                const valueLabel = spectator.query(byTestId('dot-key-value-label'));
                const valueLabelHTML = valueLabel.innerHTML.trim();
                expect(valueLabelHTML).toBe(PASSWORD_PLACEHOLDER);
            });

            it('should load the component with edit icon and switch button disabled', () => {
                const inputSwitch = spectator.query(InputSwitch);
                const editIconButton = spectator.query(
                    '[data-testId="dot-key-value-edit-button"] > button'
                ) as HTMLButtonElement;
                const valueLabel = spectator.query(byTestId('dot-key-value-label'));
                const valueLabelHTML = valueLabel.innerHTML.trim();

                expect(inputSwitch.disabled).toBeTruthy();
                expect(inputSwitch.checked()).toBeTruthy();
                expect(editIconButton.disabled).toBeTruthy();
                expect(valueLabelHTML).toBe(PASSWORD_PLACEHOLDER);
            });
        });
    });
});
