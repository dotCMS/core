import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotIconButtonModule } from '../../../../view/components/_common/dot-icon-button/dot-icon-button.module';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import {
    DotFieldVariablesServiceMock,
    mockFieldVariables
} from '../../../../test/field-variable-service.mock';
import { DotFieldVariablesService } from './services/dot-field-variables.service';
import { TableModule } from 'primeng/table';
import { of } from 'rxjs';
// tslint:disable-next-line:max-line-length
import { DotContentTypeFieldsVariablesTableRowModule } from './components/dot-content-type-fields-variables-table-row/dot-content-type-fields-variables-table-row.module';

fdescribe('ContentTypeFieldsVariablesComponent', () => {
    let comp: DotContentTypeFieldsVariablesComponent;
    let fixture: ComponentFixture<DotContentTypeFieldsVariablesComponent>;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.field.variables.actions_header.label': 'Actions',
            'contenttypes.field.variables.value_header.label': 'Value',
            'contenttypes.field.variables.key_header.label': 'Key',
            'contenttypes.field.variables.value_no_rows.label': 'No Rows',
            'contenttypes.action.save': 'Save',
            'contenttypes.action.cancel': 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotContentTypeFieldsVariablesComponent],
            imports: [
                DotIconButtonModule,
                DotActionButtonModule,
                RouterTestingModule,
                TableModule,
                DotContentTypeFieldsVariablesTableRowModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: DotFieldVariablesService,
                    useClass: DotFieldVariablesServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotContentTypeFieldsVariablesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotFieldVariableService = de.injector.get(DotFieldVariablesService);

        comp.field = {
            contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
            fieldId: mockFieldVariables[0].fieldId
        };
    });

    it('should load the component with no rows', () => {
        spyOn(dotFieldVariableService, 'load').and.returnValue(of([]));

        fixture.detectChanges();

        const dataTable = de.query(By.css('p-table'));
        expect(dataTable.nativeElement.innerText).toContain('Key');
        expect(dataTable.nativeElement.innerText).toContain('Value');
        expect(dataTable.nativeElement.innerText).toContain('Actions');
        expect(dataTable.nativeElement.innerText).toContain('No Rows');
    });

    it('should load the component with data', () => {
        fixture.detectChanges();
        const dataTable = de.query(By.css('p-table'));
        expect(dataTable.componentInstance.value).toEqual(mockFieldVariables);
        expect(dataTable.listeners[0].name).toBe('keydown.enter');
    });

    it('should create an empty variable', () => {
        fixture.detectChanges();
        de.query(By.css('.action-header__primary-button')).triggerEventHandler('click', { stopPropagation: () => {} });
        expect(comp.fieldVariables.length).toBe(4);
    });

    it('should save a variable', () => {
        spyOn(dotFieldVariableService, 'save').and.returnValue(of(mockFieldVariables[0]));
        const params = {
            contentTypeId: comp.field.contentTypeId,
            fieldId: comp.field.fieldId,
            variable: mockFieldVariables[0]
        };

        fixture.detectChanges();
        comp.saveVariable(0);
        expect(dotFieldVariableService.save).toHaveBeenCalledWith(params);
    });

    it('should delete a variable', () => {
        spyOn(dotFieldVariableService, 'delete').and.returnValue(of([]));
        fixture.detectChanges();

        const params = {
            contentTypeId: comp.field.contentTypeId,
            fieldId: comp.field.fieldId,
            variable: mockFieldVariables[0]
        };
        comp.deleteVariable(mockFieldVariables[0], 0);

        expect(dotFieldVariableService.delete).toHaveBeenCalledWith(params);
    });

    it('should stoppropagation of keydown.enter event in the datatable', () => {
        fixture.detectChanges();
        const stopPropagationSpy = jasmine.createSpy('spy');
        const dataTable = de.query(By.css('p-table'));
        dataTable.triggerEventHandler('keydown.enter', {
            stopPropagation: stopPropagationSpy
        });

        expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
    });

});
