import { By } from '@angular/platform-browser';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
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
import { DotContentTypeFieldsVariablesTableRowComponent } from './dot-content-type-fields-variables-table-row.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { mockFieldVariables } from '@tests/field-variable-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { PrimeTemplate } from 'primeng/primeng';
import { DotMessageDisplayService } from '@components/dot-message-display/services';

@Directive({
    // tslint:disable-next-line:directive-selector
    selector: '[pEditableColumn]'
})
class MockEditableColumnDirective {
    @Input('pEditableColumn')
    public pEditableColumn: any;
    @Input('pEditableColumnField')
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

    constructor(public tableRow: DotContentTypeFieldsVariablesTableRowComponent) {}

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

function showEditMenu(
    fixture: ComponentFixture<DotContentTypeFieldsVariablesTableRowComponent>
) {
    fixture.componentInstance.fieldVariable = { key: '', value: '' };
    fixture.detectChanges();
    fixture.debugElement.query(By.css('.field-variable-key-input')).triggerEventHandler('focus', {
        target: { value: '' }
    });
}

describe('DotContentTypeFieldsVariablesTableRowComponent', () => {
    let comp: DotContentTypeFieldsVariablesTableRowComponent;
    let fixture: ComponentFixture<DotContentTypeFieldsVariablesTableRowComponent>;
    let de: DebugElement;
    let dotMessageDisplayService: DotMessageDisplayService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.field.variables.key_input.placeholder': 'Enter Key',
            'contenttypes.field.variables.value_input.placeholder': 'Enter Value',
            'contenttypes.action.save': 'Save',
            'contenttypes.action.cancel': 'Cancel',
            'contenttypes.field.variables.error.duplicated.variable': 'test {0}'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotContentTypeFieldsVariablesTableRowComponent,
                MockCellEditorComponent,
                MockEditableColumnDirective
            ],
            imports: [DotIconButtonModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotMessageDisplayService
            ]
        }).compileComponents();

        fixture = DOTTestBed.createComponent(DotContentTypeFieldsVariablesTableRowComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
        comp.fieldVariable = mockFieldVariables[0];
        comp.variableIndex = 0;
    });

    it('should load the component', () => {
        showEditMenu(fixture);
        fixture.detectChanges();
        const inputs = de.queryAll(By.css('input'));
        const btns = de.queryAll(By.css('button'));
        expect(inputs[0].nativeElement.placeholder).toContain('Enter Key');
        expect(inputs[1].nativeElement.placeholder).toContain('Enter Value');
        expect(btns[0].nativeElement.innerText).toContain('CANCEL');
        expect(btns[1].nativeElement.innerText).toContain('SAVE');
        expect(comp.saveDisabled).toBe(true);
    });

    it('should focus on "Key" input when an empty variable is added', fakeAsync(() => {
        comp.fieldVariable = { key: '', value: '' };
        fixture.detectChanges();
        spyOn(comp.keyCell.nativeElement, 'click');
        tick();
        expect(comp.saveDisabled).toBe(false);
        expect(comp.keyCell.nativeElement.click).toHaveBeenCalled();
    }));

    it('should focus on "Value" input when "Edit" button clicked', () => {
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.valueCell.nativeElement, 'click');
        fixture.detectChanges();
        de.queryAll(
            By.css('.content-type-fields__variables-actions dot-icon-button')
        )[1].triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        expect(comp.valueCell.nativeElement.click).toHaveBeenCalled();
    });

    it('should show edit menu when focus/key.up on a field', () => {
        showEditMenu(fixture);
        expect(comp.rowActiveHighlight).toBe(true);
        expect(comp.showEditMenu).toBe(true);
        expect(comp.saveDisabled).toBe(true);
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('key.up', {
            target: { value: '' }
        });
        expect(comp.rowActiveHighlight).toBe(true);
        expect(comp.showEditMenu).toBe(true);
        expect(comp.saveDisabled).toBe(true);
    });

    it('should focus on "Value" field, if entered valid "Key"', () => {
        comp.fieldVariable = { key: 'test', value: '' };
        fixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.valueCell);
    });

    it('should focus on "Key" field, if entered invalid "Key"', () => {
        comp.fieldVariable = { key: '', value: '' };
        fixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.keyCell);
    });

    it('should focus on "Save" button, if entered valid "Key" & "Value"', () => {
        showEditMenu(fixture);
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        fixture.detectChanges();
        de.query(By.css('.field-variable-value-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.saveButton);
    });

    it('should emit cancel event when press "Escape"', () => {
        showEditMenu(fixture);
        spyOn(comp.cancel, 'emit');
        fixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Escape' })
        );
        expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should disabled save button when new variable key added is duplicated', () => {
        showEditMenu(fixture);
        comp.fieldVariable = { key: 'Key1', value: '' };
        comp.variablesList = [comp.fieldVariable, ...mockFieldVariables];
        fixture.detectChanges();
        comp.keyCell.nativeElement.dispatchEvent(new MouseEvent('click'));
        spyOn(dotMessageDisplayService, 'push');
        fixture.detectChanges();
        const inputKey = de.query(By.css('.field-variable-key-input')).nativeElement;
        inputKey.value = 'Key1';
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('blur', {
            type: 'blur',
            target: { value: 'Key1' }
        });
        const saveBtn = de.query(By.css('.content-type-fields__variables-actions-edit-save'))
            .nativeElement;
        fixture.detectChanges();
        expect(saveBtn.disabled).toBe(true);
        expect(dotMessageDisplayService.push).toHaveBeenCalled();
    });

    it('should emit save event when button clicked', () => {
        showEditMenu(fixture);
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.save, 'emit');
        fixture.detectChanges();
        de.query(By.css('.content-type-fields__variables-actions-edit-save')).triggerEventHandler(
            'click',
            {}
        );
        expect(comp.save.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should emit cancel event when button clicked', () => {
        showEditMenu(fixture);
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.cancel, 'emit');
        fixture.detectChanges();
        de.query(By.css('.content-type-fields__variables-actions-edit-cancel')).triggerEventHandler(
            'click',
            { stopPropagation: () => {} }
        );
        expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should emit delete event when button clicked', () => {
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.delete, 'emit');
        fixture.detectChanges();
        de.queryAll(
            By.css('.content-type-fields__variables-actions dot-icon-button')
        )[0].triggerEventHandler('click', {});
        expect(comp.delete.emit).toHaveBeenCalledWith(comp.variableIndex);
    });
});
