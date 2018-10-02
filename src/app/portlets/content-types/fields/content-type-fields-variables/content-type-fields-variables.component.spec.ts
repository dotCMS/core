import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Output, EventEmitter } from '@angular/core';
import { ContentTypeFieldsVariablesComponent, FieldVariable } from './content-type-fields-variables.component';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotIconButtonModule } from '../../../../view/components/_common/dot-icon-button/dot-icon-button.module';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { FieldVariablesServiceMock, mockFieldVariables } from '../../../../test/field-variable-service.mock';
import { FieldVariablesService } from '../service/field-variables.service';
import { Observable } from 'rxjs/Observable';

@Component({
    selector: 'dot-add-variable-form',
    template: ''
})
class AddVariableFormComponent {
    @Output() saveVariable = new EventEmitter<FieldVariable>();
    constructor() {}
}

describe('ContentTypeFieldsVariablesComponent', () => {
    let comp: ContentTypeFieldsVariablesComponent;
    let fixture: ComponentFixture<ContentTypeFieldsVariablesComponent>;
    let de: DebugElement;
    let fieldVariableService: FieldVariablesService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.field.variables.actions_header.label': 'Actions',
            'contenttypes.field.variables.value_header.label': 'Value',
            'contenttypes.field.variables.key_header.label': 'Key',
            'contenttypes.field.variables.value_no_rows.label': 'No Rows'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ContentTypeFieldsVariablesComponent, AddVariableFormComponent],
            imports: [DotIconButtonModule, RouterTestingModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: FieldVariablesService,
                    useClass: FieldVariablesServiceMock
                },
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsVariablesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        fieldVariableService = de.injector.get(FieldVariablesService);

        comp.field = {
            contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
            fieldId: mockFieldVariables[0].fieldId
        };

    });

    it('should load the component with no rows', () => {
        spyOn(fieldVariableService, 'load').and.returnValue(Observable.of([]));

        fixture.detectChanges();

        const dataTable = de.query(By.css('p-dataTable'));
        expect(dataTable.componentInstance.columns[0].header).toBe('Key');
        expect(dataTable.componentInstance.columns[1].header).toBe('Value');
        expect(dataTable.componentInstance.columns[2].header).toBe('Actions');
        expect(dataTable.nativeElement.innerText).toContain('No Rows');
        expect(dataTable.componentInstance.editable).toBe(true);
    });

    it('should load the component with data', () => {
        fixture.detectChanges();
        const dataTable = de.query(By.css('p-dataTable'));
        expect(dataTable.componentInstance.value).toEqual(mockFieldVariables);
    });

    it('should load the component and create', () => {
        spyOn(fieldVariableService, 'save');
        const params = {
            contentTypeId: comp.field.contentTypeId,
            fieldId: comp.field.fieldId,
            variable: mockFieldVariables[0]
        };

        fixture.detectChanges();
        const addVariableForm = de.query(By.css('dot-add-variable-form')).componentInstance;
        addVariableForm.saveVariable.emit(mockFieldVariables[0]);
        expect(fieldVariableService.save).toHaveBeenCalledWith(params);
    });

    it('should load the component and update', () => {
        spyOn(fieldVariableService, 'save');
        fixture.detectChanges();

        const buttons = fixture.debugElement.queryAll(By.css('button'));
        buttons[0].nativeElement.click();
        fixture.detectChanges();

        const params = {
            contentTypeId: comp.field.contentTypeId,
            fieldId: comp.field.fieldId,
            variable: mockFieldVariables[0]
        };

        expect(fieldVariableService.save).toHaveBeenCalledWith(params);
    });

    it('should load the component and delete', () => {
        spyOn(fieldVariableService, 'delete');
        fixture.detectChanges();

        const buttons = fixture.debugElement.queryAll(By.css('button'));
        buttons[1].nativeElement.click();
        fixture.detectChanges();

        const params = {
            contentTypeId: comp.field.contentTypeId,
            fieldId: comp.field.fieldId,
            variable: mockFieldVariables[0]
        };

        expect(fieldVariableService.delete).toHaveBeenCalledWith(params);
    });

});
