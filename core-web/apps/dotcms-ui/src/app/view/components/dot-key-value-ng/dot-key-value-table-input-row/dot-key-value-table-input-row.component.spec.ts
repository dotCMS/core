import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { InputSwitchModule } from 'primeng/inputswitch';

import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';

import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row.component';

import { mockKeyValue } from '../dot-key-value-ng.component.spec';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-key-value-table-input-row
            [autoFocus]="autoFocus"
            [showHiddenField]="showHiddenField"
            [variablesList]="variablesList"
        >
        </dot-key-value-table-input-row>
    `
})
class TestHostComponent {
    @Input() autoFocus: boolean;
    @Input() showHiddenField: boolean;
    @Input() variablesList: DotKeyValue[];
}

describe('DotKeyValueTableInputRowComponent', () => {
    let comp: DotKeyValueTableInputRowComponent;
    let hostComponent: TestHostComponent;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotMessageDisplayService: DotMessageDisplayService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'keyValue.key_input.placeholder': 'Enter Key',
            'keyValue.value_input.placeholder': 'Enter Value',
            Save: 'Save',
            Cancel: 'Cancel',
            'keyValue.error.duplicated.variable': 'test {0}'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotKeyValueTableInputRowComponent, TestHostComponent],
            imports: [InputSwitchModule, DotMessagePipe],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotMessageDisplayService
            ]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        hostComponent = hostComponentfixture.componentInstance;
        comp = hostComponentfixture.debugElement.query(
            By.css('dot-key-value-table-input-row')
        ).componentInstance;
        de = hostComponentfixture.debugElement.query(By.css('dot-key-value-table-input-row'));

        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
        hostComponent.variablesList = mockKeyValue;
        hostComponent.autoFocus = true;
    });

    describe('Without Hidden Fields', () => {
        it('should load the component', async () => {
            hostComponentfixture.detectChanges();
            const inputs = de.queryAll(By.css('input'));
            const btns = de.queryAll(By.css('button'));
            de.query(By.css('.field-value-input')).triggerEventHandler('focus', {});

            await hostComponentfixture.whenStable();

            expect(inputs[0].nativeElement.placeholder).toContain('Enter Key');
            expect(inputs[1].nativeElement.placeholder).toContain('Enter Value');
            expect(btns[0].nativeElement.innerText).toContain('Cancel');
            expect(btns[1].nativeElement.innerText).toContain('Save');
            expect(comp.saveDisabled).toBe(true);
        });

        it('should focus on "Key" input when loaded', async () => {
            spyOn(comp.keyCell.nativeElement, 'focus');

            hostComponentfixture.detectChanges();
            hostComponentfixture.whenStable();
            await expect(comp.keyCell.nativeElement.focus).toHaveBeenCalledTimes(1);
        });

        it('should not focus on "Key" input when loaded', async () => {
            hostComponent.autoFocus = false;
            spyOn(comp.keyCell.nativeElement, 'focus');
            hostComponentfixture.detectChanges();
            await hostComponentfixture.whenStable();
            expect(comp.keyCell.nativeElement.focus).toHaveBeenCalledTimes(0);
        });

        it('should focus on "Value" field, if entered valid "Key"', async () => {
            spyOn(comp.valueCell.nativeElement, 'focus');
            comp.variable = { key: 'test', value: '' };
            hostComponentfixture.detectChanges();
            de.query(By.css('.field-key-input')).nativeElement.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Enter' })
            );

            await hostComponentfixture.whenStable();
            expect(comp.valueCell.nativeElement.focus).toHaveBeenCalledTimes(1);
        });

        it('should focus on "Key" field, if entered invalid "Key"', async () => {
            spyOn(comp.keyCell.nativeElement, 'focus');
            comp.variable = { key: '', value: '' };
            hostComponentfixture.detectChanges();
            de.query(By.css('.field-key-input')).nativeElement.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Enter' })
            );

            await hostComponentfixture.whenStable();
            expect(comp.keyCell.nativeElement.focus).toHaveBeenCalledTimes(2);
        });

        it('should emit cancel event when press "Escape"', async () => {
            comp.variable = mockKeyValue[0];
            hostComponentfixture.detectChanges();
            hostComponentfixture.detectChanges();
            de.query(By.css('.field-value-input')).nativeElement.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Escape' })
            );

            await hostComponentfixture.whenStable();
            expect(comp.variable).toEqual({ key: '', hidden: false, value: '' });
        });

        it('should disabled save button when new variable key added is duplicated', () => {
            comp.variable = { key: 'name', value: '' };
            comp.variablesList = [comp.variable, ...mockKeyValue];
            hostComponentfixture.detectChanges();
            spyOn(dotMessageDisplayService, 'push');
            de.query(By.css('.field-key-input')).triggerEventHandler('blur', {
                type: 'blur',
                target: { value: 'Key1' }
            });
            hostComponentfixture.detectChanges();
            const saveBtn = de.query(
                By.css('.dot-key-value-table-input-row__variables-actions-edit-save')
            ).nativeElement;
            hostComponentfixture.detectChanges();
            expect(saveBtn.disabled).toBe(true);
            expect(dotMessageDisplayService.push).toHaveBeenCalled();
        });

        it('should emit save event when button clicked', async () => {
            comp.variable = { key: 'Key1', value: 'Value1' };
            spyOn(comp.save, 'emit');
            spyOn(comp.keyCell.nativeElement, 'focus');

            hostComponentfixture.detectChanges();
            await hostComponentfixture.whenStable();

            de.query(
                By.css('.dot-key-value-table-input-row__variables-actions-edit-save')
            ).triggerEventHandler('click', {});
            expect(comp.keyCell.nativeElement.focus).toHaveBeenCalled();
            expect(comp.variable).toEqual({ key: '', hidden: false, value: '' });
            expect(comp.save.emit).toHaveBeenCalledWith({ key: 'Key1', value: 'Value1' });
        });

        it('should emit cancel event when button clicked', async () => {
            comp.variable = { key: 'Key1', value: 'Value1' };
            spyOn(comp.save, 'emit');
            spyOn(comp.keyCell.nativeElement, 'focus');
            hostComponentfixture.detectChanges();
            await hostComponentfixture.whenStable();

            de.query(
                By.css('.dot-key-value-table-input-row__variables-actions-edit-cancel')
            ).triggerEventHandler('click', {
                stopPropagation: () => {
                    //
                }
            });
            expect(comp.variable).toEqual({ key: '', hidden: false, value: '' });
            expect(comp.keyCell.nativeElement.focus).toHaveBeenCalled();
        });
    });

    describe('With Hidden Fields', () => {
        beforeEach(() => {
            hostComponent.showHiddenField = true;
        });

        it('should load the component with switch button', async () => {
            hostComponentfixture.detectChanges();
            const switchButton = de.query(By.css('p-inputSwitch'));
            await hostComponentfixture.whenStable();
            expect(comp.saveDisabled).toBe(true);
            expect(switchButton).toBeTruthy();
        });

        it('should switch to hidden mode when clicked on the hidden switch button', async () => {
            comp.variable = { key: 'TestKey', hidden: true, value: 'TestValue' };
            hostComponentfixture.detectChanges();
            const valueInput = de.query(By.css('.field-value-input'));
            const switchButton = de.query(By.css('p-inputSwitch')).nativeElement;
            switchButton.dispatchEvent(new Event('click'));
            hostComponentfixture.detectChanges();
            await hostComponentfixture.whenStable();
            expect(valueInput.nativeElement.type).toBe('password');
        });
    });
});
