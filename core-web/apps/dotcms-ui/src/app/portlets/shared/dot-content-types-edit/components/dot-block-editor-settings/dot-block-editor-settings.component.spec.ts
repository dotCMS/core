import { DebugElement, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { of } from 'rxjs';
import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import {
    DotBlockEditorSettingsComponent,
    BLOCK_EDITOR_BLOCKS
} from './dot-block-editor-settings.component';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { FormBuilder, ReactiveFormsModule, FormsModule } from '@angular/forms';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

describe('DotContentTypeFieldsVariablesComponent', () => {
    let fixture: ComponentFixture<DotBlockEditorSettingsComponent>;
    let component: DotBlockEditorSettingsComponent;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;

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
                            ])
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {}
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBlockEditorSettingsComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotFieldVariableService = de.injector.get(DotFieldVariablesService);
    }));

    it('should not setup form values', () => {
        spyOn(dotFieldVariableService, 'load').and.returnValue(of([]));
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
        spyOn(component.changeControls, 'emit');
        component.ngOnChanges({
            isVisible: new SimpleChange(false, true, false)
        });
        fixture.detectChanges();
        expect(component.changeControls.emit).toHaveBeenCalled();
    });

    it('should emit valid output on form change', () => {
        spyOn(component.valid, 'emit');
        fixture.detectChanges();
        component.form.get('allowedBlocks').setValue('codeblock');
        expect(component.valid.emit).toHaveBeenCalled();
    });

    it('should emit valid output on form change', () => {
        spyOn(component.valid, 'emit');
        fixture.detectChanges();
        component.form.get('allowedBlocks').setValue('codeblock');
        expect(component.valid.emit).toHaveBeenCalled();
    });

    describe('MultiSelector', () => {
        let multiselect: MultiSelect;
        beforeEach(() => {
            fixture.detectChanges();
            multiselect = de.query(By.css('p-multiSelect')).componentInstance;
        });

        it('should have a multiselect', () => {
            expect(multiselect).toBeTruthy();
        });

        it('should have append to bobdy', () => {
            expect(multiselect.appendTo).toEqual('body');
        });

        it('should have BLOCK_EDITOR_BLOCKS options', () => {
            expect(multiselect.options).toEqual(BLOCK_EDITOR_BLOCKS);
        });
    });
});
