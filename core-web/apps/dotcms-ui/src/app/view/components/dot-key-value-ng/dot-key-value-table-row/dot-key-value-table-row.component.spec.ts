import { CommonModule, NgIf } from '@angular/common';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';

import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';

import { mockKeyValue } from '../dot-key-value-ng.component.spec';

const PASSWORD_PLACEHOLDER = '*****';
const VARIABLE_INDEX = 1;
const MESSAGE_MOCK_SERVICE = new MockDotMessageService({
    'keyValue.key_input.placeholder': 'Enter Key',
    'keyValue.value_input.placeholder': 'Enter Value',
    Save: 'Save',
    Cancel: 'Cancel',
    'keyValue.error.duplicated.variable': 'test {0}'
});

const triggerEditButtonClick = (de: DebugElement) => {
    const button = de.query(By.css('[data-testId="dot-key-value-edit-button"]'));
    button.triggerEventHandler('click', {
        stopPropagation: () => {
            //
        }
    });
};

const triggerEditableColumnClick = (de: DebugElement) => {
    const editableColumn = de.query(By.css('[data-testId="dot-key-value-editable-column"]'));
    editableColumn.nativeElement.click();
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <p-table #table [value]="variablesList">
            <ng-template pTemplate="body" let-variable let-rowIndex="rowIndex">
                <dot-key-value-table-row
                    [showHiddenField]="showHiddenField"
                    [isHiddenField]="isHiddenField"
                    [variable]="variable"
                    [variableIndex]="variableIndex"
                    [variablesList]="variablesList">
                </dot-key-value-table-row>
            </ng-template>
        </p-table>
    `
})
class TestHostComponent {
    @Input() showHiddenField: boolean;
    @Input() isHiddenField: boolean;
    @Input() variable: DotKeyValue;
    @Input() variableIndex = VARIABLE_INDEX; // default value
    @Input() variablesList: DotKeyValue[];
}

describe('DotKeyValueTableRowComponent', () => {
    let hostComponent: TestHostComponent;
    let hostComponentFixture: ComponentFixture<TestHostComponent>;
    let component: DotKeyValueTableRowComponent;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotKeyValueTableRowComponent, TestHostComponent],
            imports: [
                NgIf,
                CommonModule,
                FormsModule,
                ButtonModule,
                InputSwitchModule,
                ButtonModule,
                TableModule,
                InputSwitchModule,
                InputTextModule,
                TableModule,
                DotPipesModule,
                DotMessagePipe,
                NoopAnimationsModule
            ],
            providers: [{ provide: DotMessageService, useValue: MESSAGE_MOCK_SERVICE }]
        });

        hostComponentFixture = TestBed.createComponent(TestHostComponent);
        hostComponent = hostComponentFixture.componentInstance;
        hostComponentFixture.detectChanges();
    });

    describe('Editable variables', () => {
        beforeEach(async () => {
            hostComponent.isHiddenField = false;
            hostComponent.variable = mockKeyValue[0];
            hostComponent.variablesList = [mockKeyValue[0]];
            hostComponentFixture.detectChanges();
            await hostComponentFixture.whenStable();
            de = hostComponentFixture.debugElement.query(By.css('dot-key-value-table-row'));
            component = de.componentInstance;
        });

        it('should load the component', () => {
            const btns = de.queryAll(By.css('button'));
            const cellEditor = de.query(By.css('p-cellEditor'));
            const label = de.query(By.css('[data-testId="dot-editable-key-value"]'));

            expect(btns.length).toBe(2);
            expect(label.nativeElement).toBeDefined();
            expect(cellEditor.nativeElement).toBeDefined();
            expect(component.saveDisabled).toBe(false);
        });

        it('should focus on "Value" input when "Edit" button clicked', () => {
            const valueCellSpy = spyOn(component.valueCell.nativeElement, 'click');
            triggerEditButtonClick(de);

            hostComponentFixture.detectChanges();
            expect(valueCellSpy).toHaveBeenCalled();
        });

        it('should show edit menu when focus/key.up on a field', async () => {
            // Initial state
            expect(component.showEditMenu).toBe(false);
            expect(component.saveDisabled).toBe(false);

            // Click on edit button
            triggerEditButtonClick(de);

            // After click on edit button
            hostComponentFixture.detectChanges();
            await hostComponentFixture.whenStable();

            // Focus on field
            const field = de.query(By.css('[data-testId="dot-key-value-input"]'));
            field.triggerEventHandler('keyup', { target: { value: 'a' } });

            hostComponentFixture.detectChanges();

            // After focus on field
            expect(component.showEditMenu).toBe(true);
            expect(component.saveDisabled).toBe(false);
        });

        describe('showHiddenField is true', () => {
            beforeEach(async () => {
                hostComponent.showHiddenField = true;
                hostComponentFixture.detectChanges();
                await hostComponentFixture.whenStable();
            });

            it('should switch to hidden mode when clicked on the hidden switch button', async () => {
                expect(component.variableCopy.hidden).toBeFalsy();

                // Get the input switch element
                const inputSwitch = de.query(By.css('[data-testId="dot-key-value-hidden-switch"]'));
                const innerInputElement = inputSwitch.query(By.css('.p-inputswitch')).nativeElement;
                innerInputElement.click();

                hostComponentFixture.detectChanges();
                await hostComponentFixture.whenStable();

                // Click on edit button
                triggerEditableColumnClick(de);

                // After click on edit button
                hostComponentFixture.detectChanges();
                await hostComponentFixture.whenStable();

                const inputElement = de.query(
                    By.css('[data-testId="dot-key-value-input"]')
                ).nativeElement;
                expect(inputElement.type).toBe('password');
                expect(component.showEditMenu).toBe(true);
            });
        });

        describe('Edit Input field is visible', () => {
            beforeEach(async () => {
                triggerEditButtonClick(de);
                hostComponentFixture.detectChanges();
                await hostComponentFixture.whenStable();
            });

            it('should emit cancel event when press "Escape"', () => {
                const cancelSpy = spyOn(component.cancel, 'emit');
                const inputElement = de.query(
                    By.css('[data-testId="dot-key-value-input"]')
                ).nativeElement;
                inputElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
                hostComponentFixture.detectChanges();

                expect(cancelSpy).toHaveBeenCalledWith(component.variableIndex);
                expect(component.showEditMenu).toBe(false);
            });

            it('should emit save event when button clicked', () => {
                const saveSpy = spyOn(component.save, 'emit');

                // focus on field to show edit menu
                const inputElement = de.query(By.css('[data-testId="dot-key-value-input"]'));
                inputElement.triggerEventHandler('focus', {});

                hostComponentFixture.detectChanges();

                // click on save button
                const saveButton = de.query(By.css('[data-testId="dot-key-value-save-button"]'));
                saveButton.triggerEventHandler('click', {});

                hostComponentFixture.detectChanges();

                expect(saveSpy).toHaveBeenCalledWith(mockKeyValue[0]);
                expect(component.showEditMenu).toBe(false);
            });

            it('should emit cancel event when button clicked', () => {
                const cancelSpy = spyOn(component.cancel, 'emit');

                // focus on field to show edit menu
                const inputElement = de.query(By.css('[data-testId="dot-key-value-input"]'));
                inputElement.triggerEventHandler('focus', {});

                hostComponentFixture.detectChanges();

                // click on save button
                const cancelButton = de.query(
                    By.css('[data-testId="dot-key-value-cancel-button"]')
                );
                cancelButton.triggerEventHandler('click', {
                    stopPropagation: () => {
                        //
                    }
                });

                hostComponentFixture.detectChanges();

                expect(cancelSpy).toHaveBeenCalledWith(VARIABLE_INDEX);
                expect(component.showEditMenu).toBe(false);
            });

            it('should emit delete event when button clicked', () => {
                const deleteSpy = spyOn(component.delete, 'emit');

                // click on delete button
                const deleteButton = de.query(
                    By.css('[data-testId="dot-key-value-delete-button"]')
                );
                deleteButton.triggerEventHandler('click', {});

                hostComponentFixture.detectChanges();

                expect(deleteSpy).toHaveBeenCalledWith(component.variable);
            });
        });

        describe('With Hidden Fields', () => {
            beforeEach(async () => {
                hostComponent.isHiddenField = true;
                hostComponent.showHiddenField = true;
                hostComponent.variable = mockKeyValue[1]; // Hidden Key Value
                hostComponent.variablesList = [mockKeyValue[1]]; // Hidden Key Value
                hostComponentFixture.detectChanges();
                await hostComponentFixture.whenStable();
                de = hostComponentFixture.debugElement.query(By.css('dot-key-value-table-row'));
                component = de.componentInstance;
            });

            it('should show the password placeholder instead of the key value', () => {
                const valueLabel = de.query(By.css('[data-testId="dot-key-value-label"]'));
                const valueLabelHTML = valueLabel.nativeElement.innerHTML.trim();
                expect(valueLabelHTML).toBe(PASSWORD_PLACEHOLDER);
            });

            it('should load the component with edit icon and switch button disabled', () => {
                const switchButton = de.query(
                    By.css("[data-testId='dot-key-value-hidden-switch']")
                );
                const editIconButton = de.query(
                    By.css("[data-testId='dot-key-value-edit-button']")
                );
                const valueLabel = de.query(By.css("[data-testId='dot-key-value-label']"));
                const valueLabelHTML = valueLabel.nativeElement.innerHTML.trim();

                expect(switchButton.componentInstance.disabled).toBe(true);
                expect(editIconButton.componentInstance.disabled).toBe(true);
                expect(valueLabelHTML).toBe(PASSWORD_PLACEHOLDER);
            });
        });
    });
});
