import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotContentTypeFieldsVariablesTableRowModule } from './components/dot-content-type-fields-variables-table-row/dot-content-type-fields-variables-table-row.module';
import { DotContentTypeFieldsVariablesTableRowComponent } from './components/dot-content-type-fields-variables-table-row/dot-content-type-fields-variables-table-row.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { LoginService } from 'dotcms-js';
import { DotMessageService } from '@services/dot-messages-service';
import { DotFieldVariablesService } from './services/dot-field-variables.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { LoginServiceMock } from '@tests/login-service.mock';
import {
    DotFieldVariablesServiceMock,
    mockFieldVariables
} from '@tests/field-variable-service.mock';
import { TableModule } from 'primeng/table';
import { of } from 'rxjs';
import * as _ from 'lodash';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';
import { DotMessageDisplayService } from '@components/dot-message-display/services';

describe('DotContentTypeFieldsVariablesComponent', () => {
    let comp: DotContentTypeFieldsVariablesComponent;
    let fixture: ComponentFixture<DotContentTypeFieldsVariablesComponent>;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;
    let tableRow: DotContentTypeFieldsVariablesTableRowComponent;

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
                },
                DotMessageDisplayService
            ]
        });

        fixture = DOTTestBed.createComponent(DotContentTypeFieldsVariablesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotFieldVariableService = de.injector.get(DotFieldVariablesService);

        comp.field = {
            ...dotcmsContentTypeFieldBasicMock,
            contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
            id: mockFieldVariables[0].fieldId
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
        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        expect(tableRow.variablesList).toEqual(mockFieldVariables);
        expect(dataTable.componentInstance.value).toEqual(mockFieldVariables);
        expect(dataTable.listeners[0].name).toBe('keydown.enter');
    });

    it('should create an empty variable', () => {
        fixture.detectChanges();
        de.query(By.css('.action-header__primary-button')).triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        expect(comp.fieldVariables.length).toBe(4);
        expect(comp.fieldVariablesBackup.length).toBe(4);
    });

    it('should save a variable', () => {
        spyOn(dotFieldVariableService, 'save').and.returnValue(of(mockFieldVariables[0]));

        fixture.detectChanges();
        comp.fieldVariablesBackup[0] = _.cloneDeep(mockFieldVariables[1]);
        comp.fieldVariablesBackup[2] = _.cloneDeep(mockFieldVariables[0]);

        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        tableRow.save.emit(1);
        expect(dotFieldVariableService.save).toHaveBeenCalledWith(
            comp.field,
            mockFieldVariables[1]
        );
        expect(comp.fieldVariablesBackup[0]).not.toEqual(comp.fieldVariables[0]);
        expect(comp.fieldVariablesBackup[1]).toEqual(comp.fieldVariables[1]);
        expect(comp.fieldVariablesBackup[2]).not.toEqual(comp.fieldVariables[2]);
    });

    it('should delete a variable from the server', () => {
        spyOn(dotFieldVariableService, 'delete').and.returnValue(of([]));
        fixture.detectChanges();

        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        tableRow.delete.emit(0);

        expect(dotFieldVariableService.delete).toHaveBeenCalledWith(
            comp.field,
            mockFieldVariables[0]
        );
    });

    it('should delete an empty variable from the UI', () => {
        spyOn(dotFieldVariableService, 'load').and.returnValue(
            of([
                {
                    key: 'test',
                    value: 'none'
                }
            ])
        );
        fixture.detectChanges();

        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        tableRow.delete.emit(0);

        expect(comp.fieldVariables.length).toBe(0);
    });

    it('should delete an empty variable from the UI, when cancelled', () => {
        spyOn(dotFieldVariableService, 'load').and.returnValue(
            of([
                {
                    key: 'test',
                    value: 'none'
                }
            ])
        );
        fixture.detectChanges();

        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        tableRow.cancel.emit(0);

        expect(comp.fieldVariables.length).toBe(0);
    });

    it('should restore original value to variable in the UI, when cancelled', () => {
        fixture.detectChanges();

        comp.fieldVariablesBackup[0].value = 'Value Changed';

        tableRow = de.query(By.css('dot-content-type-fields-variables-table-row'))
            .componentInstance;
        tableRow.cancel.emit(0);

        expect(comp.fieldVariablesBackup[0]).toEqual(comp.fieldVariables[0]);
    });

    it('should stop propagation of keydown.enter event in the datatable', () => {
        fixture.detectChanges();
        const stopPropagationSpy = jasmine.createSpy('spy');
        const dataTable = de.query(By.css('p-table'));
        dataTable.triggerEventHandler('keydown.enter', {
            stopPropagation: stopPropagationSpy
        });

        expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
    });
});
