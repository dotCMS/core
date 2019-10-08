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
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotFieldVariable } from '../../models/dot-field-variable.interface';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-content-type-fields-variables-table-row
            [fieldVariable]="fieldVariable"
            [variableIndex]="variableIndex"
            [variablesList]="variablesList"
        >
        </dot-content-type-fields-variables-table-row>
    `
})
class TestHostComponent {
    @Input() fieldVariable: DotFieldVariable;
    @Input() variableIndex: number;
    @Input() variablesList: DotFieldVariable[];
}

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
    let hostComponent: TestHostComponent;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
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
                MockEditableColumnDirective,
                TestHostComponent
            ],
            imports: [DotIconButtonModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotMessageDisplayService
            ]
        }).compileComponents();

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        hostComponent = hostComponentfixture.componentInstance;
        comp = hostComponentfixture.debugElement.query(
            By.css('dot-content-type-fields-variables-table-row')
        ).componentInstance;
        de = hostComponentfixture.debugElement.query(
            By.css('dot-content-type-fields-variables-table-row')
        );

        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
        hostComponent.variableIndex = 0;
        hostComponent.variablesList = mockFieldVariables;
    });

    it('should load the component', () => {
        hostComponent.fieldVariable = mockFieldVariables[0];
        hostComponentfixture.detectChanges();
        const inputs = de.queryAll(By.css('input'));
        const btns = de.queryAll(By.css('button'));
        expect(inputs[0].nativeElement.placeholder).toContain('Enter Value');
        expect(btns[0].nativeElement.innerText).toContain('delete_outline');
        expect(btns[1].nativeElement.innerText).toContain('edit');
        expect(comp.saveDisabled).toBe(false);
    });

    it('should focus on "Key" input when an empty variable is added', (done) => {
        hostComponent.fieldVariable = {
            key: '',
            value: ''
        };
        hostComponentfixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('focus', {});
        spyOn(comp.keyCell.nativeElement, 'click');
        hostComponentfixture.detectChanges();
        setTimeout(() => {
            expect(comp.saveDisabled).toBe(true);
            expect(comp.keyCell.nativeElement.click).toHaveBeenCalled();
            done();
        }, 0);
    });

    it('should focus on "Value" input when "Edit" button clicked', () => {
        hostComponent.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.valueCell.nativeElement, 'click');
        hostComponentfixture.detectChanges();
        de.queryAll(
            By.css('.content-type-fields__variables-actions dot-icon-button')
        )[1].triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        hostComponentfixture.detectChanges();
        expect(comp.valueCell.nativeElement.click).toHaveBeenCalled();
    });

    it('should show edit menu when focus/key.up on a field', () => {
        hostComponent.fieldVariable = mockFieldVariables[0];
        hostComponentfixture.detectChanges();
        expect(comp.rowActiveHighlight).toBe(false);
        expect(comp.showEditMenu).toBe(false);
        expect(comp.saveDisabled).toBe(false);
        de.query(By.css('.field-variable-value-input')).triggerEventHandler('keyup', {
            target: { value: 'a' }
        });
        hostComponentfixture.detectChanges();
        expect(comp.rowActiveHighlight).toBe(true);
        expect(comp.showEditMenu).toBe(true);
        expect(comp.saveDisabled).toBe(false);
    });

    it('should focus on "Value" field, if entered valid "Key"', () => {
        hostComponent.fieldVariable = { key: 'test', value: '' };
        hostComponentfixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.valueCell);
    });

    it('should focus on "Key" field, if entered invalid "Key"', () => {
        hostComponent.fieldVariable = { key: '', value: '' };
        hostComponentfixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter' })
        );
        expect(comp.elemRef).toBe(comp.keyCell);
    });

    it('should emit cancel event when press "Escape"', () => {
        hostComponent.fieldVariable = mockFieldVariables[0];
        hostComponentfixture.detectChanges();
        spyOn(comp.cancel, 'emit');
        hostComponentfixture.detectChanges();
        de.query(By.css('.field-variable-value-input')).nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Escape' })
        );
        expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should disabled save button when new variable key added is duplicated', () => {
        hostComponent.fieldVariable = { key: 'Key1', value: '' };
        hostComponent.variablesList = [hostComponent.fieldVariable, ...mockFieldVariables];
        spyOn(dotMessageDisplayService, 'push');
        hostComponentfixture.detectChanges();
        de.query(By.css('.field-variable-key-input')).triggerEventHandler('blur', {
            type: 'blur',
            target: { value: 'Key1' }
        });
        hostComponentfixture.detectChanges();
        const saveBtn = de.query(By.css('.content-type-fields__variables-actions-edit-save'))
            .nativeElement;
        hostComponentfixture.detectChanges();
        expect(saveBtn.disabled).toBe(true);
        expect(dotMessageDisplayService.push).toHaveBeenCalled();
    });

    it('should emit save event when button clicked and not modify "isEditing" variable when component gets updated', () => {
        hostComponent.fieldVariable = { key: 'Key1', value: 'Value1' };
        hostComponentfixture.detectChanges();
        spyOn(comp.save, 'emit');
        de.query(By.css('.field-variable-value-input')).triggerEventHandler('focus', {});
        hostComponentfixture.detectChanges();
        de.query(By.css('.content-type-fields__variables-actions-edit-save')).triggerEventHandler(
            'click',
            {}
        );
        hostComponent.variablesList = [];
        hostComponentfixture.detectChanges();
        expect(comp.save.emit).toHaveBeenCalledWith(comp.variableIndex);
        expect(comp.isEditing).toBe(true);
    });

    it('should emit cancel event when button clicked', () => {
        hostComponent.fieldVariable = { key: 'Key1', value: 'Value1' };
        hostComponentfixture.detectChanges();
        spyOn(comp.save, 'emit');
        de.query(By.css('.field-variable-value-input')).triggerEventHandler('focus', {});
        spyOn(comp.cancel, 'emit');
        hostComponentfixture.detectChanges();
        de.query(By.css('.content-type-fields__variables-actions-edit-cancel')).triggerEventHandler(
            'click',
            { stopPropagation: () => {} }
        );
        expect(comp.cancel.emit).toHaveBeenCalledWith(comp.variableIndex);
    });

    it('should emit delete event when button clicked', () => {
        hostComponent.fieldVariable = { key: 'TestKey', value: 'TestValue' };
        spyOn(comp.delete, 'emit');
        hostComponentfixture.detectChanges();
        de.queryAll(
            By.css('.content-type-fields__variables-actions dot-icon-button')
        )[0].triggerEventHandler('click', {});
        expect(comp.delete.emit).toHaveBeenCalledWith(comp.variableIndex);
    });
});
