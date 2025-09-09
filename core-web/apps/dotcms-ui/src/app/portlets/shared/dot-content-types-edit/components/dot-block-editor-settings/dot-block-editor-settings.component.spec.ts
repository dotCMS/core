import { of, throwError } from 'rxjs';

import { CommonModule } from '@angular/common';
import { DebugElement, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { getEditorBlockOptions } from '@dotcms/block-editor';
import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, mockFieldVariables } from '@dotcms/utils-testing';

import { DotBlockEditorSettingsComponent } from './dot-block-editor-settings.component';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

describe('DotContentTypeFieldsVariablesComponent', () => {
    let fixture: ComponentFixture<DotBlockEditorSettingsComponent>;
    let component: DotBlockEditorSettingsComponent;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let amountFields;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.dropzone.action.save': 'Save',
        'contenttypes.dropzone.action.cancel': 'Cancel'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotBlockEditorSettingsComponent],
            imports: [MultiSelectModule, CommonModule, FormsModule, ReactiveFormsModule],
            providers: [
                FormBuilder,
                {
                    provide: DotFieldVariablesService,
                    useValue: {
                        load: () =>
                            of([
                                {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                                    fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                                    id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                                    key: 'allowedBlocks',
                                    value: 'orderList,unorderList,table'
                                }
                            ]),
                        save: () => of([]),
                        delete: () => of([])
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: () => of([])
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBlockEditorSettingsComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotFieldVariableService = de.injector.get(DotFieldVariablesService);
        dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);
        amountFields = component.settings.length;
    }));

    it('should not setup form values', () => {
        jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of([]));
        fixture.detectChanges();
        expect(component.form.get('allowedBlocks').value).toBe(null);
    });

    it('should setup from value', () => {
        const value = ['orderList', 'unorderList', 'table'];
        fixture.detectChanges();
        const selector = de.query(By.css('p-multiSelect'));
        expect(component.form.get('allowedBlocks').value).toEqual(value);
        expect(selector).toBeTruthy();
    });

    it('should emit changeControls when isVisible input is true', () => {
        fixture.detectChanges();
        jest.spyOn(component.changeControls, 'emit');
        component.ngOnChanges({
            isVisible: new SimpleChange(false, true, false)
        });
        fixture.detectChanges();
        expect(component.changeControls.emit).toHaveBeenCalled();
    });

    it('should emit valid output on form change', () => {
        jest.spyOn(component.valid, 'emit');
        fixture.detectChanges();
        component.form.get('allowedBlocks').setValue(['codeblock']);
        expect(component.valid.emit).toHaveBeenCalled();
    });

    it('should save properties on saveSettings', () => {
        jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(of(mockFieldVariables[0]));
        jest.spyOn(component.save, 'emit');
        fixture.detectChanges();
        component.saveSettings();
        expect(dotFieldVariableService.save).toHaveBeenCalledTimes(amountFields);
        expect(component.save.emit).toHaveBeenCalled();
        expect(component.settingsMap['allowedBlocks'].variable).toEqual(mockFieldVariables[0]);
    });

    it('should delete properties on saveSettings when is empty', () => {
        jest.spyOn(dotFieldVariableService, 'delete').mockReturnValue(of(mockFieldVariables[0]));
        jest.spyOn(component.save, 'emit');
        fixture.detectChanges();
        component.form.get('allowedBlocks').setValue([]);
        component.saveSettings();
        expect(dotFieldVariableService.delete).toHaveBeenCalled();
        expect(component.save.emit).toHaveBeenCalled();
        expect(component.settingsMap['allowedBlocks'].variable).toEqual(mockFieldVariables[0]);
    });

    it('should not call save or delete when is empty and not previus vairable exist', () => {
        jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of([]));
        jest.spyOn(dotFieldVariableService, 'delete');
        jest.spyOn(dotFieldVariableService, 'save');
        fixture.detectChanges();
        component.form.get('allowedBlocks').setValue([]);
        component.saveSettings();
        expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
        expect(dotFieldVariableService.save).not.toHaveBeenCalled();
    });

    it('should handler error if save proprties faild', () => {
        jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(throwError({}));
        jest.spyOn(dotHttpErrorManagerService, 'handle').mockReturnValue(of());
        jest.spyOn(component.save, 'emit');
        fixture.detectChanges();
        component.saveSettings();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        expect(component.save.emit).not.toHaveBeenCalled();
    });

    describe('MultiSelector', () => {
        let multiselect: MultiSelect;

        beforeEach(() => {
            fixture.detectChanges();
            multiselect = de.query(By.css('p-multiSelect')).componentInstance;
        });

        it('should have append to bobdy', () => {
            expect(multiselect.appendTo).toEqual('body');
        });

        it('should have Editor Block Options options', () => {
            expect(multiselect.options).toEqual(getEditorBlockOptions());
        });
    });

    describe('Options', () => {
        it('should not have a "paragraph" option', () => {
            const options = component.settingsMap.allowedBlocks.options;
            const paragraphOption = options.find(
                ({ label, code }) =>
                    code.trim().toLowerCase() === 'paragraph' ||
                    label.trim().toLowerCase() === 'paragraph'
            );

            expect(paragraphOption).not.toBeDefined();
        });
    });
});
