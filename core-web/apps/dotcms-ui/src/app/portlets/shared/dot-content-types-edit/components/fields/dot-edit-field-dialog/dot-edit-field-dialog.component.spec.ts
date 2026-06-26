import { Component, input, output, Renderer2 } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TabsModule } from 'primeng/tabs';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotDialogActions,
    DotFieldVariable
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    cleanUpDialog,
    dotcmsContentTypeBasicMock,
    dotcmsContentTypeFieldBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotEditFieldDialogComponent, DotEditFieldDialogData } from '.';

import { FieldPropertyService } from '../service/field-properties.service';

const fakeContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    id: '1234567890',
    name: 'ContentTypeName',
    variable: 'helloVariable',
    baseType: 'testBaseType'
};

@Component({
    selector: 'dot-content-type-fields-properties-form',
    template: ''
})
class TestContentTypeFieldsPropertiesFormComponent {
    saveField = output<DotCMSContentTypeField>();
    valid = output<boolean>();
    formFieldData = input<DotCMSContentTypeField>();
    contentType = input<DotCMSContentType>();

    form = new FormGroup({});

    saveFieldProperties = jest.fn();

    public destroy(): void {
        return;
    }
}

@Component({
    selector: 'dot-convert-to-block-info',
    template: ''
})
class DotConvertToBlockInfoStubComponent {
    currentFieldType = input<unknown>();
    currentField = input<unknown>();
}

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    template: ''
})
class DotConvertWysiwygToBlockStubComponent {
    currentFieldType = input<unknown>();
}

@Component({
    selector: 'dot-block-editor-settings',
    template: ''
})
class DotBlockEditorSettingsStubComponent {
    field = input<DotCMSContentTypeField>();
    isVisible = input(false);
    $changeControls = output<DotDialogActions>();
    $valid = output<boolean>();
    $save = output<DotFieldVariable[]>();
}

@Component({
    selector: 'dot-binary-settings',
    template: ''
})
class DotBinarySettingsStubComponent {
    field = input<DotCMSContentTypeField>();
    isVisible = input(false);
    $changeControls = output<DotDialogActions>();
    $valid = output<boolean>();
    $save = output<DotFieldVariable[]>();
}

@Component({
    selector: 'dot-custom-field-settings',
    template: ''
})
class DotCustomFieldSettingsStubComponent {
    field = input<DotCMSContentTypeField>();
    isVisible = input(false);
    renderMode = input<string>();
    $changeControls = output<DotDialogActions>();
    $valid = output<boolean>();
    $save = output<void>();
}

@Component({
    selector: 'dot-content-type-fields-variables',
    template: ''
})
class DotContentTypeFieldsVariablesStubComponent {
    field = input<DotCMSContentTypeField>();
    showTable = input(false);
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.dropzone.action.save': 'Save',
    'contenttypes.dropzone.action.cancel': 'Cancel',
    'contenttypes.dropzone.tab.overview': 'Overview',
    'contenttypes.dropzone.tab.settings': 'Settings',
    'contenttypes.dropzone.tab.variables': 'Variables'
});

const TEXT_FIELD_TYPE = {
    id: 'text',
    label: 'Text',
    clazz: DotCMSClazzes.TEXT,
    helpText: '',
    properties: []
};

const BLOCK_EDITOR_FIELD_TYPE = {
    id: 'block-editor',
    label: 'Block Editor',
    clazz: DotCMSClazzes.BLOCK_EDITOR,
    helpText: '',
    properties: []
};

function setup(data: Partial<DotEditFieldDialogData>) {
    const refMock = { close: jest.fn() };

    TestBed.configureTestingModule({
        declarations: [DotEditFieldDialogComponent],
        imports: [
            BrowserAnimationsModule,
            ReactiveFormsModule,
            DotMessagePipe,
            TabsModule,
            ButtonModule,
            TestContentTypeFieldsPropertiesFormComponent,
            DotConvertToBlockInfoStubComponent,
            DotConvertWysiwygToBlockStubComponent,
            DotBlockEditorSettingsStubComponent,
            DotBinarySettingsStubComponent,
            DotCustomFieldSettingsStubComponent,
            DotContentTypeFieldsVariablesStubComponent
        ],
        providers: [
            FieldPropertyService,
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogConfig, useValue: { data } },
            { provide: DynamicDialogRef, useValue: refMock }
        ]
    });

    const fixture = TestBed.createComponent(DotEditFieldDialogComponent);
    const comp = fixture.componentInstance;
    const originalDetectChanges = fixture.detectChanges.bind(fixture);
    fixture.detectChanges = () => originalDetectChanges(false);

    return { fixture, comp, refMock };
}

describe('DotEditFieldDialogComponent', () => {
    beforeEach(() => {
        // Mock matchMedia for PrimeNG components
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    describe('create (field without id)', () => {
        let fixture: ComponentFixture<DotEditFieldDialogComponent>;
        let comp: DotEditFieldDialogComponent;
        let refMock: { close: jest.Mock };

        beforeEach(waitForAsync(() => {
            ({ fixture, comp, refMock } = setup({
                currentField: { ...dotcmsContentTypeFieldBasicMock, clazz: DotCMSClazzes.TEXT },
                currentFieldType: TEXT_FIELD_TYPE,
                contentType: fakeContentType
            }));
            fixture.detectChanges();
        }));

        afterEach(() => {
            cleanUpDialog(fixture);
        });

        it('should disable the Save button on init', () => {
            expect(comp.saveBtn.disabled).toBeTruthy();
        });

        it('should pass the contentType to the properties form', () => {
            const form = fixture.debugElement.query(
                ({ name }) => name === 'dot-content-type-fields-properties-form'
            );
            expect(form.componentInstance.contentType()?.name).toBe('ContentTypeName');
        });

        it('should enable/disable Save through setDialogOkButtonState', () => {
            comp.setDialogOkButtonState(true);
            expect(comp.saveBtn.disabled).toBe(false);

            comp.setDialogOkButtonState(false);
            expect(comp.saveBtn.disabled).toBe(true);
        });

        it('should replace Save button with accept controls in changesDialogActions', () => {
            const accept = {
                action: () => {
                    /* */
                },
                label: 'Save',
                disabled: true
            };
            comp.changesDialogActions({ accept, cancel: { label: 'Cancel' } });
            expect(comp.saveBtn).toEqual(accept);
        });

        it('should disable the variables tab when there is no field id', () => {
            const variablesTabDisabled = !comp.currentField?.id;
            expect(variablesTabDisabled).toBe(true);
        });

        it('should call ref.close with saved result onPropertiesSaved', () => {
            const field = { ...dotcmsContentTypeFieldBasicMock, id: 'new-id' };
            comp.onPropertiesSaved(field);
            expect(refMock.close).toHaveBeenCalledWith({ kind: 'saved', field });
        });

        it('should call ref.close with no argument from cancelBtn.action', () => {
            comp.cancelBtn.action();
            expect(refMock.close).toHaveBeenCalledWith();
        });

        it('should call saveFieldProperties from saveBtn.action', () => {
            comp.saveBtn.action();
            expect(comp.$propertiesForm().saveFieldProperties).toHaveBeenCalled();
        });

        it('should report variablesTabIndex as 1 for a plain text field', () => {
            expect(comp.isFieldWithSettings).toBe(false);
            expect(comp.variablesTabIndex).toBe(1);
        });
    });

    describe('edit (field with id)', () => {
        let fixture: ComponentFixture<DotEditFieldDialogComponent>;
        let comp: DotEditFieldDialogComponent;
        let refMock: { close: jest.Mock };

        beforeEach(waitForAsync(() => {
            ({ fixture, comp, refMock } = setup({
                currentField: {
                    ...dotcmsContentTypeFieldBasicMock,
                    id: '123',
                    clazz: DotCMSClazzes.BLOCK_EDITOR
                },
                currentFieldType: BLOCK_EDITOR_FIELD_TYPE,
                contentType: fakeContentType
            }));
            fixture.detectChanges();
        }));

        afterEach(() => {
            cleanUpDialog(fixture);
        });

        it('should NOT disable the variables tab when there is a field id', () => {
            const variablesTabDisabled = !comp.currentField?.id;
            expect(variablesTabDisabled).toBe(false);
        });

        it('should report isFieldWithSettings true and variablesTabIndex 2 for block editor with id', () => {
            expect(comp.isFieldWithSettings).toBe(true);
            expect(comp.variablesTabIndex).toBe(2);
        });

        it('should track overview changes only when active tab is Overview', () => {
            comp.activeTab = comp.OVERVIEW_TAB_INDEX;
            comp.setDialogOkButtonState(true);

            // Move to Settings and toggle the button there: overview state must be preserved
            comp.activeTab = comp.SETTINGS_TAB_INDEX;
            comp.setDialogOkButtonState(false);

            // Switching back to Overview restores the enabled state
            comp.handleTabChange(comp.OVERVIEW_TAB_INDEX);
            expect(comp.saveBtn.disabled).toBe(false);
        });

        it('should keep Save enabled when switching to Settings and back after a change', () => {
            comp.activeTab = comp.OVERVIEW_TAB_INDEX;
            comp.setDialogOkButtonState(true);
            expect(comp.saveBtn.disabled).toBe(false);

            comp.handleTabChange(comp.SETTINGS_TAB_INDEX);
            comp.handleTabChange(comp.OVERVIEW_TAB_INDEX);
            expect(comp.saveBtn.disabled).toBe(false);
        });

        it('should keep Save disabled when switching to Settings and back with no change', () => {
            comp.activeTab = comp.OVERVIEW_TAB_INDEX;
            comp.setDialogOkButtonState(false);

            comp.handleTabChange(comp.SETTINGS_TAB_INDEX);
            comp.handleTabChange(comp.OVERVIEW_TAB_INDEX);
            expect(comp.saveBtn.disabled).toBe(true);
        });

        it('should hide the buttons when switching to the variables tab', () => {
            comp.handleTabChange(comp.variablesTabIndex);
            expect(comp.hideButtons).toBe(true);
        });

        it('should NOT hide the buttons on the Settings tab for a field with settings', () => {
            comp.handleTabChange(comp.SETTINGS_TAB_INDEX);
            expect(comp.hideButtons).toBe(false);
        });

        it('should call ref.close with settings-saved onSettingsSaved', () => {
            comp.onSettingsSaved();
            expect(refMock.close).toHaveBeenCalledWith({ kind: 'settings-saved' });
        });
    });

    describe('WYSIWYG field (convert to block)', () => {
        let fixture: ComponentFixture<DotEditFieldDialogComponent>;
        let comp: DotEditFieldDialogComponent;
        let refMock: { close: jest.Mock };

        beforeEach(waitForAsync(() => {
            ({ fixture, comp, refMock } = setup({
                currentField: {
                    ...dotcmsContentTypeFieldBasicMock,
                    id: '3',
                    contentTypeId: '3b',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
                },
                currentFieldType: {
                    id: 'wysiwyg',
                    label: 'WYSIWYG',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                    helpText: '',
                    properties: []
                },
                contentType: fakeContentType
            }));
            fixture.detectChanges();
        }));

        afterEach(() => {
            cleanUpDialog(fixture);
        });

        it('should close with convert-to-block result onConvertToBlock', () => {
            comp.onConvertToBlock();
            expect(refMock.close).toHaveBeenCalledWith({
                kind: 'convert-to-block',
                field: expect.objectContaining({
                    id: '3',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                    fieldType: 'Story-Block'
                })
            });
        });

        it('should scroll the convert-to-block section into view via scrollTo', () => {
            const rendered = fixture.debugElement.injector.get(Renderer2);
            const scrollIntoViewSpy = jest.fn();
            jest.spyOn(rendered, 'selectRootElement').mockReturnValue({
                scrollIntoView: scrollIntoViewSpy
            });

            comp.scrollTo();

            expect(rendered.selectRootElement).toHaveBeenCalledWith(
                'dot-convert-wysiwyg-to-block',
                true
            );
            expect(scrollIntoViewSpy).toHaveBeenCalledWith({
                behavior: 'smooth',
                block: 'start',
                inline: 'nearest'
            });
        });
    });
});
