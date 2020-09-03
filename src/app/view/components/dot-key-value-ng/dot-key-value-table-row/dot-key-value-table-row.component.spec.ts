import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import {
    DebugElement,
    Component,
    Directive,
    Input,
    AfterContentInit,
    ContentChildren,
    QueryList,
    TemplateRef
} from '@angular/core';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { PrimeTemplate, InputSwitchModule } from 'primeng/primeng';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';
import { mockKeyValue } from '../dot-key-value-ng.component.spec';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-key-value-table-row
            [showHiddenField]="showHiddenField"
            [isHiddenField]="isHiddenField"
            [variable]="variable"
            [variableIndex]="variableIndex"
            [variablesList]="variablesList"
        >
        </dot-key-value-table-row>
    `
})
class TestHostComponent {
    @Input() showHiddenField: boolean;
    @Input() isHiddenField: boolean;
    @Input() variable: DotKeyValue;
    @Input() variableIndex: number;
    @Input() variablesList: DotKeyValue[];
}

@Directive({
    // tslint:disable-next-line:directive-selector
    selector: '[pEditableColumn]'
})
class MockEditableColumnDirective {
    @Input()
    public pEditableColumn: any;
    @Input()
    public pEditableColumnField: any;
}

@Component({
    // tslint:disable-next-line:component-selector
    selector: 'p-cellEditor',
    template: `
        <ng-container>
            <ng-container *ngTemplateOutlet="inputTemplate"></ng-container>
        </ng-container>
    `
})
class MockCellEditorComponent implements AfterContentInit {
    @ContentChildren(PrimeTemplate) templates: QueryList<PrimeTemplate>;
    inputTemplate: TemplateRef<any>;
    outputTemplate: TemplateRef<any>;

    constructor(public tableRow: DotKeyValueTableRowComponent) {}

    ngAfterContentInit() {
        this.templates.forEach((item) => {
            switch (item.getType()) {
                case 'input':
                    this.inputTemplate = item.template;
                    break;
                case 'output':
                    this.outputTemplate = item.template;
                    break;
            }
        });
    }
}

describe('DotKeyValueTableRowComponent', () => {
    let comp: DotKeyValueTableRowComponent;
    let hostComponent: TestHostComponent;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'keyValue.key_input.placeholder': 'Enter Key',
            'keyValue.value_input.placeholder': 'Enter Value',
            Save: 'Save',
            Cancel: 'Cancel',
            'keyValue.error.duplicated.variable': 'test {0}'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotKeyValueTableRowComponent,
                MockCellEditorComponent,
                MockEditableColumnDirective,
                TestHostComponent
            ],
            imports: [DotIconButtonModule, InputSwitchModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        hostComponent = hostComponentfixture.componentInstance;
        comp = hostComponentfixture.debugElement.query(By.css('dot-key-value-table-row'))
            .componentInstance;
        de = hostComponentfixture.debugElement.query(By.css('dot-key-value-table-row'));

        hostComponent.variableIndex = 0;
        hostComponent.variablesList = mockKeyValue;
    });

    describe('Without Hidden Fields', () => {
        it('should load the component', () => {
            hostComponent.variableIndex = 1;
            hostComponent.variable = mockKeyValue[0];
            hostComponentfixture.detectChanges();
            const inputs = de.queryAll(By.css('input'));
            const btns = de.queryAll(By.css('button'));
            expect(inputs[0].nativeElement.placeholder).toContain('Enter Value');
            expect(btns[0].nativeElement.innerText).toContain('delete_outline');
            expect(btns[1].nativeElement.innerText).toContain('edit');
            expect(comp.saveDisabled).toBe(false);
        });

        it('should focus on "Value" input when "Edit" button clicked', () => {
            hostComponent.variableIndex = 1;
            hostComponent.variable = { key: 'TestKey', value: 'TestValue' };
            hostComponentfixture.detectChanges();
            spyOn(comp.valueCell.nativeElement, 'click');
            const button = de.queryAll(
                By.css('.dot-key-value-table-row__variables-actions dot-icon-button')
            )[1];
            button.triggerEventHandler('click', {
                stopPropagation: () => {}
            });
            hostComponentfixture.detectChanges();
            expect(comp.valueCell.nativeElement.click).toHaveBeenCalled();
        });

        it('should show edit menu when focus/key.up on a field', () => {
            hostComponent.variable = mockKeyValue[0];
            hostComponentfixture.detectChanges();
            expect(comp.showEditMenu).toBe(false);
            expect(comp.saveDisabled).toBe(false);
            de.query(By.css('.field-value-input')).triggerEventHandler('keyup', {
                target: { value: 'a' }
            });
            hostComponentfixture.detectChanges();
            expect(comp.showEditMenu).toBe(true);
            expect(comp.saveDisabled).toBe(false);
        });

        it('should emit cancel event when press "Escape"', () => {
            hostComponent.variable = mockKeyValue[0];
            hostComponentfixture.detectChanges();
            spyOn(comp.cancel, 'emit');
            hostComponentfixture.detectChanges();
            de.query(By.css('.field-value-input')).nativeElement.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Escape' })
            );
            expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
            expect(comp.showEditMenu).toBe(false);
        });

        it('should emit save event when button clicked', () => {
            hostComponent.variable = { key: 'Key1', value: 'Value1' };
            hostComponentfixture.detectChanges();
            spyOn(comp.save, 'emit');
            de.query(By.css('.field-value-input')).triggerEventHandler('focus', {});
            hostComponentfixture.detectChanges();
            hostComponentfixture.whenStable().then(() => {
                de.query(
                    By.css('.dot-key-value-table-row__variables-actions-edit-save')
                ).triggerEventHandler('click', {});
                hostComponent.variablesList = [];
                hostComponentfixture.detectChanges();
                expect(comp.save.emit).toHaveBeenCalledWith(comp.variable);
                expect(comp.showEditMenu).toBe(false);
            });
        });

        it('should emit cancel event when button clicked', () => {
            hostComponent.variable = { key: 'Key1', value: 'Value1' };
            hostComponentfixture.detectChanges();
            spyOn(comp.save, 'emit');
            de.query(By.css('.field-value-input')).triggerEventHandler('focus', {});
            spyOn(comp.cancel, 'emit');
            hostComponentfixture.detectChanges();
            de.query(
                By.css('.dot-key-value-table-row__variables-actions-edit-cancel')
            ).triggerEventHandler('click', { stopPropagation: () => {} });
            expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
            expect(comp.showEditMenu).toBe(false);
        });

        it('should emit delete event when button clicked', () => {
            hostComponent.variable = { key: 'TestKey', value: 'TestValue' };
            spyOn(comp.delete, 'emit');
            hostComponentfixture.detectChanges();
            de.queryAll(
                By.css('.dot-key-value-table-row__variables-actions dot-icon-button')
            )[0].triggerEventHandler('click', {});
            expect(comp.delete.emit).toHaveBeenCalledWith(comp.variable);
        });
    });

    describe('With Hidden Fields', () => {
        beforeEach(() => {
            hostComponent.showHiddenField = true;
            hostComponent.variableIndex = 1;
            hostComponent.variable = mockKeyValue[1];
        });

        it('should load the component with edit icon and switch button disabled', () => {
            hostComponent.isHiddenField = true;
            hostComponentfixture.detectChanges();
            const switchButton = de.query(By.css('p-inputSwitch'));
            const valueLabel = de.queryAll(By.css('.dot-key-value-table-row td'))[1];
            const editIconButton = de.queryAll(
                By.css('.dot-key-value-table-row__variables-actions dot-icon-button')
            )[1];
            expect(switchButton.componentInstance.disabled).toBe(true);
            expect(editIconButton.attributes.disabled).toBe('true');
            expect(valueLabel.nativeElement.innerText).toContain('*');
        });

        it('should switch to hidden mode when clicked on the hidden switch button', () => {
            hostComponent.isHiddenField = false;
            hostComponent.variable = { key: 'TestKey', hidden: true, value: 'TestValue' };
            hostComponentfixture.detectChanges();
            const valueInput = de.query(By.css('.field-value-input'));
            const switchButton = de.query(By.css('p-inputSwitch')).nativeElement;
            switchButton.dispatchEvent(new Event('onChange'));
            hostComponentfixture.detectChanges();
            hostComponentfixture.whenStable().then(() => {
                expect(comp.showEditMenu).toBe(true);
                expect(valueInput.nativeElement.type).toBe('password');
            });
        });
    });
});
