import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule, ToggleSwitch } from 'primeng/toggleswitch';

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

/**
 * Test template without pReorderableRow / pReorderableRowHandle so we don't need PrimeNG Table in the test injector.
 * Same structure and behavior, only the drag directives are omitted.
 */
const TEST_TEMPLATE = `
@let variable = $variable();
@let showHiddenField = $showHiddenField();
@let isHiddenField = $isHiddenField();
<tr class="dot-key-value-table-row">
    @if ($dragAndDrop()) {
        <td class="p-2 align-middle">
            <span class="pi pi-bars text-gray-500"></span>
        </td>
    }
    <td class="p-2 align-middle" data-testId="dot-key-value-key">
        <span>{{ variable.key }}</span>
    </td>
    @if (isHiddenField) {
        <td class="p-2 align-middle" data-testId="dot-key-value-label">
            <span>
                <i class="pi pi-lock inline-block mr-1"></i>
                {{ 'keyValue.value_hidden' | dm }}
            </span>
        </td>
    } @else {
        <td class="p-2 align-middle" data-testId="dot-key-value-editable-column">
            <input
                (keydown.enter)="onPressEnter($event)"
                [placeholder]="'keyValue.value_input.placeholder' | dm"
                [type]="inputType"
                [formControl]="valueControl"
                class="w-full"
                autocomplete="false"
                data-testId="dot-key-value-input"
                pInputText
                pSize="small" />
        </td>
    }
    @if (showHiddenField) {
        <td class="p-2 align-middle">
            @if (valueControl.value !== passwordPlaceholder && !variable.hidden) {
                <p-toggleSwitch
                    [formControl]="hiddenControl"
                    data-testId="dot-key-value-hidden-switch" />
            }
        </td>
    }
    <td class="p-2 align-middle">
        <p-button
            (click)="delete.emit()"
            data-testId="dot-key-value-delete-button"
            icon="pi pi-times"
            severity="secondary"
            size="small"
            [text]="true" />
    </td>
</tr>
`;

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
            }
        ],
        overrideComponents: [[DotKeyValueTableRowComponent, { set: { template: TEST_TEMPLATE } }]]
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

                const inputSwitch = spectator.query(ToggleSwitch);

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
