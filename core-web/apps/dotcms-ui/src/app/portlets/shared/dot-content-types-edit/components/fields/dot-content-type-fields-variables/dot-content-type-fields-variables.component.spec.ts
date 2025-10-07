import { of } from 'rxjs';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageDisplayService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSClazzes, DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';
import { EMPTY_FIELD } from '@dotcms/utils';
import {
    dotcmsContentTypeFieldBasicMock,
    DotFieldVariablesServiceMock,
    LoginServiceMock,
    mockFieldVariables
} from '@dotcms/utils-testing';

import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotFieldVariablesService } from './services/dot-field-variables.service';

import { DOTTestBed } from '../../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-content-type-fields-variables [field]="value"></dot-content-type-fields-variables>
    `,
    standalone: false
})
class TestHostComponent {
    value: DotCMSContentTypeField = {
        ...dotcmsContentTypeFieldBasicMock,
        contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
        id: mockFieldVariables[0].fieldId
    };
}

describe('DotContentTypeFieldsVariablesComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let deHost: DebugElement;
    let comp: DotContentTypeFieldsVariablesComponent;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent, DotContentTypeFieldsVariablesComponent],
            imports: [DotKeyValueComponent],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: DotFieldVariablesService,
                    useClass: DotFieldVariablesServiceMock
                },
                DotMessageDisplayService
            ]
        });

        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        de = deHost.query(By.css('dot-content-type-fields-variables'));
        comp = de.componentInstance;

        dotFieldVariableService = de.injector.get(DotFieldVariablesService);
    });

    it('should load the component with one empty row', () => {
        jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of([]));
        fixtureHost.detectChanges();
        expect(comp.fieldVariables.length).toBe(0);
    });

    it('should save a variable', () => {
        jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(of(mockFieldVariables[0]));
        const response = mockFieldVariables[0];

        fixtureHost.detectChanges();

        const dotKeyValue = de.query(By.css('dot-key-value-ng'));
        dotKeyValue.triggerEventHandler('save', response);

        expect(dotFieldVariableService.save).toHaveBeenCalledWith(
            comp.field,
            mockFieldVariables[0]
        );
        expect(comp.fieldVariables[0]).toEqual(mockFieldVariables[0]);
    });

    it('should update variable a variable', () => {
        const variable = {
            ...mockFieldVariables[0],
            value: 'test'
        };

        jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(of(variable));

        fixtureHost.detectChanges();

        const dotKeyValue = de.query(By.css('dot-key-value-ng'));
        dotKeyValue.triggerEventHandler('update', {
            variable,
            oldVariable: mockFieldVariables[0]
        });

        expect(dotFieldVariableService.save).toHaveBeenCalledWith(comp.field, variable);
        expect(dotFieldVariableService.save).toHaveBeenCalledTimes(1);

        expect(comp.fieldVariables[0]).toEqual(variable);
    });

    it('should delete a variable from the server', () => {
        const variableToDelete = mockFieldVariables[0];
        jest.spyOn<DotFieldVariablesService>(dotFieldVariableService, 'delete').mockReturnValue(
            of([])
        );
        const deletedCollection = mockFieldVariables.filter(
            (item: DotFieldVariable) => variableToDelete.key !== item.key
        );
        fixtureHost.detectChanges();

        const dotKeyValue = de.query(By.css('dot-key-value-ng'));
        dotKeyValue.triggerEventHandler('delete', variableToDelete);

        expect(dotFieldVariableService.delete).toHaveBeenCalledWith(comp.field, variableToDelete);
        expect(dotFieldVariableService.delete).toHaveBeenCalledTimes(1);
        expect(comp.fieldVariables).toEqual(deletedCollection);
    });

    describe('Block Editor Field', () => {
        const BLOCK_EDITOR_FIELD: DotCMSContentTypeField = {
            ...EMPTY_FIELD,
            clazz: DotCMSClazzes.BLOCK_EDITOR
        };

        beforeEach(() => {
            fixtureHost.componentInstance.value = BLOCK_EDITOR_FIELD;
        });

        it('should set variable correctly', () => {
            jest.spyOn<DotFieldVariablesService>(dotFieldVariableService, 'load').mockReturnValue(
                of(mockFieldVariables)
            );
            fixtureHost.detectChanges();
            expect(comp.fieldVariables.length).toBe(mockFieldVariables.length);
        });

        it('should not set allowedBlocks variable', () => {
            jest.spyOn<DotFieldVariablesService>(dotFieldVariableService, 'load').mockReturnValue(
                of([
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                        id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                        key: 'allowedBlocks',
                        value: 'dotImage'
                    }
                ])
            );
            fixtureHost.detectChanges();
            expect(comp.fieldVariables.length).toBe(0);
        });
    });
});
