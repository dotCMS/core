import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator';
import { MockProvider } from 'ng-mocks';

import { fakeAsync, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';
import { InputText, InputTextModule } from 'primeng/inputtext';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row.component';

import { DotKeyValue } from '../dot-key-value-ng.component';

export const mockKeyValue: DotKeyValue[] = [
    {
        key: 'name',
        hidden: false,
        value: 'John'
    }
];

describe('DotKeyValueTableInputRowComponent', () => {
    let spectatorHost: SpectatorHost<DotKeyValueTableInputRowComponent>;
    let dotMessageDisplayService: DotMessageDisplayService;
    const createHost = createHostFactory({
        component: DotKeyValueTableInputRowComponent,
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
                    Save: 'Save',
                    Cancel: 'Cancel',
                    'keyValue.error.duplicated.variable': 'test {0}'
                })
            },
            MockProvider(DotMessageDisplayService)
        ]
    });

    beforeEach(() => {
        spectatorHost = createHost(
            `<dot-key-value-table-input-row
            [autoFocus]="autoFocus"
            [showHiddenField]="showHiddenField"
            [variablesList]="variablesList"
            [forbiddenkeys]="forbiddenkeys"
            >
        </dot-key-value-table-input-row>`,
            {
                hostProps: {
                    autoFocus: true,
                    showHiddenField: false,
                    variablesList: mockKeyValue,
                    forbiddenkeys: {
                        name: true
                    }
                },
                detectChanges: false
            }
        );
        dotMessageDisplayService = spectatorHost.inject(DotMessageDisplayService, true);
    });

    describe('Without Hidden Fields', () => {
        it('should load the component', () => {
            spectatorHost.detectChanges();
            const inputs = spectatorHost.queryAll(InputText);
            const saveButton = spectatorHost.query(
                'button[data-testId="save-button"]'
            ) as HTMLButtonElement;
            const cancelButton = spectatorHost.query(
                'button[data-testId="cancel-button"]'
            ) as HTMLButtonElement;
            const element = spectatorHost.query(byTestId('value-input'));

            const keyInput = inputs[0];
            const valueInput = inputs[1];

            element.dispatchEvent(new Event('focus'));

            expect(keyInput.el.nativeElement.placeholder).toContain('Enter Key');
            expect(valueInput.el.nativeElement.placeholder).toContain('Enter Value');
            expect(cancelButton.innerText).toContain('Cancel');
            expect(saveButton.innerText).toContain('Save');
            expect(saveButton.disabled).toBe(true);
        });

        it('should focus on "Key" input when loaded', () => {
            spectatorHost.detectChanges();
            const keyInputElement = spectatorHost.component.keyCell.nativeElement;
            expect(document.activeElement).toEqual(keyInputElement); // Check if the element is focused
        });

        it('should not focus on "Key" input when loaded', () => {
            spectatorHost.setHostInput({ autoFocus: false });
            spectatorHost.detectChanges();
            const keyInputElement = spectatorHost.component.keyCell.nativeElement;
            expect(document.activeElement).not.toEqual(keyInputElement); // Check if the element is focused
        });

        it('should focus on "Value" field, if entered a valid "Key"', () => {
            spectatorHost.detectChanges();

            const element = spectatorHost.query(byTestId('key-input')) as HTMLInputElement;
            element.value = 'valid-key';

            spectatorHost.dispatchFakeEvent(element, 'input');
            spectatorHost.detectChanges();

            element.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            const valueInputElement = spectatorHost.component.valueCell.nativeElement;
            expect(document.activeElement).toEqual(valueInputElement);
        });

        it('should focus on "Key" field, if entered an invalid "Key"', () => {
            spectatorHost.detectChanges();
            const keyInputElement = spectatorHost.query(byTestId('key-input')) as HTMLInputElement;

            keyInputElement.value = 'name';
            spectatorHost.dispatchFakeEvent(keyInputElement, 'input');

            spectatorHost.detectChanges();

            keyInputElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            const valueInputElement = spectatorHost.component.valueCell.nativeElement;
            expect(document.activeElement).not.toEqual(valueInputElement);
        });

        it('should reset form when press "Escape"', () => {
            spectatorHost.detectChanges();
            const spyForm = spyOn(spectatorHost.component.form, 'reset');
            const valueInput = spectatorHost.query(byTestId('value-input')) as HTMLInputElement;
            valueInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            expect(spyForm).toHaveBeenCalled();
        });

        it('should disabled save button when new variable key added is duplicated', fakeAsync(() => {
            spectatorHost.detectChanges();
            const spy = spyOn(dotMessageDisplayService, 'push');
            const element = spectatorHost.query(byTestId('key-input')) as HTMLInputElement;

            element.value = 'name';
            spectatorHost.dispatchFakeEvent(element, 'input');
            tick(1000);

            const saveBtn = spectatorHost.query(
                'button[data-testId="save-button"]'
            ) as HTMLButtonElement;
            expect(spy).toHaveBeenCalledWith({
                life: 3000,
                message: 'test name',
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
            expect(saveBtn.disabled).toBeTruthy();
        }));

        it('should enable save button when new variable key added is valid', () => {
            spectatorHost.detectChanges();
            const spy = spyOn(spectatorHost.component.save, 'emit');
            const keyInput = spectatorHost.query(byTestId('key-input')) as HTMLInputElement;
            const valueInput = spectatorHost.query(byTestId('value-input')) as HTMLInputElement;

            keyInput.value = 'newKey';
            spectatorHost.dispatchFakeEvent(keyInput, 'input');
            valueInput.value = 'newValue';
            spectatorHost.dispatchFakeEvent(valueInput, 'input');

            const saveBtn = spectatorHost.query(
                'button[data-testId="save-button"]'
            ) as HTMLButtonElement;

            expect(saveBtn.disabled).toBeFalsy();

            spectatorHost.click(saveBtn);

            expect(spy).toHaveBeenCalledWith({
                key: 'newKey',
                value: 'newValue',
                hidden: false
            });
        });

        it('should emit reset form when cancel button is clicked', async () => {
            spectatorHost.detectChanges();
            const form = spectatorHost.component.form;
            const spyForm = spyOn(form, 'reset');
            const keyInput = spectatorHost.query(byTestId('key-input')) as HTMLInputElement;
            const valueInput = spectatorHost.query(byTestId('value-input')) as HTMLInputElement;

            keyInput.value = 'newKey';
            spectatorHost.dispatchFakeEvent(keyInput, 'input');
            valueInput.value = 'newValue';
            spectatorHost.dispatchFakeEvent(valueInput, 'input');

            const saveBtn = spectatorHost.query(
                'button[data-testId="cancel-button"]'
            ) as HTMLButtonElement;

            expect(saveBtn.disabled).toBeFalsy();

            spectatorHost.click(saveBtn);
            expect(spyForm).toHaveBeenCalled();
        });
    });

    describe('With Hidden Fields', () => {
        beforeEach(() => {
            spectatorHost.setHostInput({ showHiddenField: true });
        });

        it('should load the component with switch button', () => {
            spectatorHost.detectChanges();
            const switchInput = spectatorHost.query(InputSwitch);
            expect(switchInput).toBeTruthy();
        });

        it('should switch to hidden mode when clicked on the hidden switch button', async () => {
            spectatorHost.detectChanges();
            const switchInput = spectatorHost.query('.p-inputswitch') as HTMLElement;

            spectatorHost.click(switchInput);
            spectatorHost.detectChanges();

            const valueInput = spectatorHost.query(byTestId('value-input')) as HTMLInputElement;
            expect(valueInput.type).toBe('password');
        });
    });
});
