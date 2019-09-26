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
import { DotContentTypeFieldsVariablesTableRowComponent } from './dot-content-type-fields-variables-table-row.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { mockFieldVariables } from '@tests/field-variable-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { PrimeTemplate } from 'primeng/primeng';

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

describe('DotContentTypeFieldsVariablesTableRowComponent', () => {
    let comp: DotContentTypeFieldsVariablesTableRowComponent;
    let fixture: ComponentFixture<DotContentTypeFieldsVariablesTableRowComponent>;
    let de: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.field.variables.key_input.placeholder': 'Enter Key',
            'contenttypes.field.variables.value_input.placeholder': 'Enter Value',
            'contenttypes.action.save': 'Save',
            'contenttypes.action.cancel': 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotContentTypeFieldsVariablesTableRowComponent,
                MockCellEditorComponent,
                MockEditableColumnDirective
            ],
            imports: [DotIconButtonModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = DOTTestBed.createComponent(DotContentTypeFieldsVariablesTableRowComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        comp.fieldVariable = mockFieldVariables[0];
        comp.variableIndex = 0;
    });

    it('should load the component', () => {
        comp.showEditMenu = true;
        fixture.detectChanges();
        const inputs = de.queryAll(By.css('input'));
        const btns = de.queryAll(By.css('button'));
        expect(inputs[0].nativeElement.placeholder).toContain('Enter Key');
        expect(inputs[1].nativeElement.placeholder).toContain('Enter Value');
        expect(btns[0].nativeElement.innerText).toContain('CANCEL');
        expect(btns[1].nativeElement.innerText).toContain('SAVE');
        expect(comp.saveDisabled).toBe(false);
    });

    xit('should focus on "Key" input when an empty variable is added', () => {
        comp.fieldVariable = { key: '', value: '' };
        spyOn(comp.valueCell.nativeElement, 'click');
        fixture.detectChanges();
        expect(comp.saveDisabled).toBe(false);
        expect(comp.keyCell.nativeElement.click).toHaveBeenCalled();
    });

    it('should focus on "Value" input when "Edit" button clicked', () => {
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        comp.showEditMenu = false;
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
        comp.fieldVariable = { key: '', value: '' };
        fixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('focus', {});
        expect(comp.rowActiveHighlight).toBe(true);
        expect(comp.showEditMenu).toBe(true);
        expect(comp.saveDisabled).toBe(true);
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('key.up', {});
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
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        comp.showEditMenu = true;
        fixture.detectChanges();
        de.query(By.css('.field-variable-value-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.saveButton);
    });

    it('should emit cancel event when press "Escape"', () => {
        comp.showEditMenu = true;
        comp.fieldVariable = { key: '', value: '' };
        spyOn(comp.cancel, 'emit');
        fixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Escape' })
        );
        expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should emit save event when button clicked', () => {
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        comp.showEditMenu = true;
        spyOn(comp.save, 'emit');
        fixture.detectChanges();
        de.query(By.css('.content-type-fields__variables-actions-edit-save')).triggerEventHandler(
            'click',
            {}
        );
        expect(comp.save.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should emit cancel event when button clicked', () => {
        comp.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        comp.showEditMenu = true;
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
        comp.showEditMenu = false;
        spyOn(comp.delete, 'emit');
        fixture.detectChanges();
        de.queryAll(
            By.css('.content-type-fields__variables-actions dot-icon-button')
        )[0].triggerEventHandler('click', {});
        expect(comp.delete.emit).toHaveBeenCalledWith(comp.variableIndex);
    });
});
