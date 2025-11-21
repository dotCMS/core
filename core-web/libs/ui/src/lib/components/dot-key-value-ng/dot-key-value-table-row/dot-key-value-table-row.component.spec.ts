import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Table, TableModule, TableService } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

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

describe('DotKeyValueTableRowComponent', () => {
    let spectator: Spectator<DotKeyValueTableRowComponent>;
    const createComponent = createComponentFactory({
        component: DotKeyValueTableRowComponent,
        imports: [
            FormsModule,
            ReactiveFormsModule,
            ToggleSwitchModule,
            InputTextModule,
            ButtonModule,
            TextareaModule,
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
                    'keyValue.value_hidden': 'Value hidden'
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
                variable: mockVariable,
                index: 0,
                dragAndDrop: false
            } as unknown
        });

        spectator.detectChanges();
    });

    describe('Editable variables', () => {
        it('should load the component', () => {
            const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));
            const valueInput = spectator.query(byTestId('dot-key-value-input'));
            const keyElement = spectator.query(byTestId('dot-key-value-key'));

            expect(deleteButton).toBeTruthy();
            expect(valueInput).toBeTruthy();
            expect(keyElement.textContent).toContain('name');
        });

        describe('Edit Input field is visible', () => {
            it('should emit save when the value is changed', fakeAsync(() => {
                const saveSpy = jest.spyOn(spectator.component.save, 'emit');
                const valueInput = spectator.query<HTMLInputElement>(
                    byTestId('dot-key-value-input')
                );

                spectator.typeInElement('newValue', valueInput);
                spectator.detectChanges();
                tick(1100);

                expect(saveSpy).toHaveBeenCalledWith({
                    ...mockVariable,
                    value: 'newValue'
                });
            }));

            it('should emit delete event when button clicked', () => {
                const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');
                const deleteButton = spectator.query(byTestId('dot-key-value-delete-button'));

                spectator.click(deleteButton);
                spectator.detectChanges();

                expect(deleteSpy).toHaveBeenCalled();
            });

            it('should emit save when Enter key is pressed', () => {
                const saveSpy = jest.spyOn(spectator.component.save, 'emit');
                const valueInput = spectator.query<HTMLInputElement>(
                    byTestId('dot-key-value-input')
                );

                spectator.typeInElement('newValue', valueInput);
                valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
                spectator.detectChanges();

                expect(saveSpy).toHaveBeenCalledWith({
                    ...mockVariable,
                    value: 'newValue',
                    hidden: false
                });
            });

            it('should set input type to text when not hidden', () => {
                const valueInput = spectator.query<HTMLInputElement>(
                    byTestId('dot-key-value-input')
                );

                expect(valueInput.type).toBe('text');
                expect(spectator.component.inputType).toBe('text');
            });
        });

        describe('Hidden Fields', () => {
            beforeEach(() => {
                spectator = createComponent({
                    props: {
                        showHiddenField: true,
                        variable: { ...mockVariable, hidden: true },
                        index: 0,
                        dragAndDrop: false
                    } as unknown
                });
                spectator.detectChanges();
            });

            it('should show the password placeholder instead of the value', () => {
                const valueLabel = spectator.query<HTMLParagraphElement>(
                    byTestId('dot-key-value-label')
                );
                expect(valueLabel.textContent).toContain('Value hidden');
            });

            it('should have disabled edit controls when field is hidden', async () => {
                await spectator.fixture.whenStable();

                expect(spectator.component.form).toBeTruthy();

                const inputSwitch = spectator.query(byTestId('dot-key-value-hidden-switch'));

                expect(inputSwitch).toBeFalsy();
                expect(spectator.component.$isHiddenField()).toBe(true);
            });

            it('should set input type to password when hidden', () => {
                // Create with non-hidden to then toggle to hidden
                spectator = createComponent({
                    props: {
                        showHiddenField: true,
                        variable: mockVariable,
                        index: 0,
                        dragAndDrop: false
                    } as unknown
                });
                spectator.detectChanges();

                // Now manually set form control to hidden
                spectator.component.hiddenControl.setValue(true);
                spectator.detectChanges();

                expect(spectator.component.inputType).toBe('password');
            });
        });

        describe('Drag and Drop', () => {
            beforeEach(() => {
                spectator = createComponent({
                    props: {
                        showHiddenField: false,
                        variable: mockVariable,
                        index: 0,
                        dragAndDrop: true
                    } as unknown
                });
                spectator.detectChanges();
            });

            it('should show the drag handle when dragAndDrop is true', () => {
                const dragHandle = spectator.query('.pi-bars');
                expect(dragHandle).toBeTruthy();
            });
        });
    });
});
