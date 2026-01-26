import { of, throwError } from 'rxjs';

import { CommonModule } from '@angular/common';
import { DebugElement, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { getEditorBlockOptions } from '@dotcms/block-editor';
import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockFieldVariables } from '@dotcms/utils-testing';

import { DotBlockEditorSettingsComponent } from './dot-block-editor-settings.component';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.dropzone.action.save': 'Save',
    'contenttypes.dropzone.action.cancel': 'Cancel'
});

const mockFieldVariablesServiceWithData = {
    load: jest.fn().mockReturnValue(
        of([
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                key: 'allowedBlocks',
                value: 'orderList,unorderList,table'
            }
        ])
    ),
    save: jest.fn().mockReturnValue(of([])),
    delete: jest.fn().mockReturnValue(of([]))
};

const mockFieldVariablesServiceEmpty = {
    load: jest.fn().mockReturnValue(of([])),
    save: jest.fn().mockReturnValue(of([])),
    delete: jest.fn().mockReturnValue(of([]))
};

const MOCK_FIELD: Partial<DotCMSContentTypeField> = {
    id: 'f965a51b-130a-435f-b646-41e07d685363',
    name: 'testField',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField'
} as unknown;

describe('DotBlockEditorSettingsComponent', () => {
    describe('with existing variables', () => {
        let fixture: ComponentFixture<DotBlockEditorSettingsComponent>;
        let component: DotBlockEditorSettingsComponent;
        let de: DebugElement;
        let dotFieldVariableService: DotFieldVariablesService;
        let dotHttpErrorManagerService: DotHttpErrorManagerService;
        let amountFields: number;

        beforeEach(waitForAsync(() => {
            // Reset mocks
            mockFieldVariablesServiceWithData.load.mockClear();
            mockFieldVariablesServiceWithData.save.mockClear();
            mockFieldVariablesServiceWithData.delete.mockClear();

            TestBed.configureTestingModule({
                declarations: [DotBlockEditorSettingsComponent],
                imports: [MultiSelectModule, CommonModule, FormsModule, ReactiveFormsModule],
                providers: [
                    FormBuilder,
                    {
                        provide: DotFieldVariablesService,
                        useValue: mockFieldVariablesServiceWithData
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
            fixture.componentRef.setInput('field', MOCK_FIELD);
            de = fixture.debugElement;
            component = de.componentInstance;
            dotFieldVariableService = de.injector.get(DotFieldVariablesService);
            dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);
            amountFields = component.settings.length;
        }));

        it('should setup form value', () => {
            const value = ['orderList', 'unorderList', 'table'];
            fixture.detectChanges();
            const selector = de.query(By.css('p-multiselect'));
            expect(component.form.get('allowedBlocks').value).toEqual(value);
            expect(selector).toBeTruthy();
        });

        it('should emit changeControls when isVisible input is true', () => {
            fixture.detectChanges();
            jest.spyOn(component.$changeControls, 'emit');
            component.ngOnChanges({
                $isVisible: new SimpleChange(false, true, false)
            });
            fixture.detectChanges();
            expect(component.$changeControls.emit).toHaveBeenCalled();
        });

        it('should emit valid output on form change', () => {
            jest.spyOn(component.$valid, 'emit');
            fixture.detectChanges();
            component.form.get('allowedBlocks').setValue(['codeblock']);
            expect(component.$valid.emit).toHaveBeenCalled();
        });

        it('should save properties on saveSettings', () => {
            mockFieldVariablesServiceWithData.save.mockReturnValue(of(mockFieldVariables[0]));
            jest.spyOn(component.$save, 'emit');
            fixture.detectChanges();
            component.saveSettings();
            expect(dotFieldVariableService.save).toHaveBeenCalledTimes(amountFields);
            expect(component.$save.emit).toHaveBeenCalled();
            expect(component.settingsMap['allowedBlocks'].variable).toEqual(mockFieldVariables[0]);
        });

        it('should delete properties on saveSettings when is empty', () => {
            mockFieldVariablesServiceWithData.delete.mockReturnValue(of(mockFieldVariables[0]));
            jest.spyOn(component.$save, 'emit');
            fixture.detectChanges();
            component.form.get('allowedBlocks').setValue([]);
            component.saveSettings();
            expect(dotFieldVariableService.delete).toHaveBeenCalled();
            expect(component.$save.emit).toHaveBeenCalled();
            expect(component.settingsMap['allowedBlocks'].variable).toEqual(mockFieldVariables[0]);
        });

        it('should handle error if save properties failed', () => {
            mockFieldVariablesServiceWithData.save.mockReturnValue(throwError(() => ({})));
            jest.spyOn(dotHttpErrorManagerService, 'handle').mockReturnValue(of());
            jest.spyOn(component.$save, 'emit');
            fixture.detectChanges();
            component.saveSettings();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            expect(component.$save.emit).not.toHaveBeenCalled();
        });

        describe('MultiSelector', () => {
            let multiselect: MultiSelect;

            beforeEach(() => {
                fixture.detectChanges();
                multiselect = de.query(By.css('p-multiselect')).componentInstance;
            });

            it('should have append to body', () => {
                const appendToValue =
                    typeof multiselect.appendTo === 'function'
                        ? (multiselect.appendTo as () => string)()
                        : multiselect.appendTo;
                expect(appendToValue).toEqual('body');
            });

            it('should have Editor Block Options options', () => {
                const optionsValue =
                    typeof multiselect.options === 'function'
                        ? (multiselect.options as () => unknown[])()
                        : multiselect.options;
                expect(optionsValue).toEqual(getEditorBlockOptions());
            });
        });
    });

    describe('without existing variables', () => {
        let fixture: ComponentFixture<DotBlockEditorSettingsComponent>;
        let component: DotBlockEditorSettingsComponent;
        let de: DebugElement;
        let dotFieldVariableService: DotFieldVariablesService;

        beforeEach(waitForAsync(() => {
            // Reset mocks
            mockFieldVariablesServiceEmpty.load.mockClear();
            mockFieldVariablesServiceEmpty.save.mockClear();
            mockFieldVariablesServiceEmpty.delete.mockClear();

            TestBed.configureTestingModule({
                declarations: [DotBlockEditorSettingsComponent],
                imports: [MultiSelectModule, CommonModule, FormsModule, ReactiveFormsModule],
                providers: [
                    FormBuilder,
                    {
                        provide: DotFieldVariablesService,
                        useValue: mockFieldVariablesServiceEmpty
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
            fixture.componentRef.setInput('field', MOCK_FIELD);
            de = fixture.debugElement;
            component = de.componentInstance;
            dotFieldVariableService = de.injector.get(DotFieldVariablesService);
        }));

        it('should not setup form values when no variables exist', () => {
            fixture.detectChanges();
            expect(component.form.get('allowedBlocks').value).toBe(null);
        });

        it('should not call save or delete when is empty and no previous variable exist', () => {
            fixture.detectChanges();
            component.form.get('allowedBlocks').setValue([]);
            component.saveSettings();
            expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
            expect(dotFieldVariableService.save).not.toHaveBeenCalled();
        });
    });

    describe('Options', () => {
        let component: DotBlockEditorSettingsComponent;

        beforeEach(waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotBlockEditorSettingsComponent],
                imports: [MultiSelectModule, CommonModule, FormsModule, ReactiveFormsModule],
                providers: [
                    FormBuilder,
                    {
                        provide: DotFieldVariablesService,
                        useValue: mockFieldVariablesServiceEmpty
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

            const fixture = TestBed.createComponent(DotBlockEditorSettingsComponent);
            fixture.componentRef.setInput('field', MOCK_FIELD);
            component = fixture.componentInstance;
        }));

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
