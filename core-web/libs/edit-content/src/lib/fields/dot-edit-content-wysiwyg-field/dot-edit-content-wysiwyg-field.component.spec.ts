import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotPropertiesService, DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { DotMessagePipe, mockMatchMedia, monacoMock } from '@dotcms/utils-testing';

import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import { DotWysiwygTinymceService } from './components/dot-wysiwyg-tinymce/service/dot-wysiwyg-tinymce.service';
import { DotEditContentWYSIWYGFieldComponent } from './dot-edit-content-wysiwyg-field.component';
import {
    AvailableEditor,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';
import { DEFAULT_IMAGE_URL_PATTERN } from './dot-wysiwyg-plugin/utils/editor.utils';
import {
    WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
    WYSIWYG_MOCK
} from './mocks/dot-edit-content-wysiwyg-field.mock';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';

const mockScrollIntoView = () => {
    Element.prototype.scrollIntoView = jest.fn();
};

const mockSystemWideConfig = { systemWideOption: 'value' };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentWYSIWYGFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentWYSIWYGFieldComponent,
        host: MockFormComponent,
        imports: [
            ReactiveFormsModule,
            DropdownModule,
            NoopAnimationsModule,
            ConfirmDialogModule,
            DotMessagePipe
        ],
        detectChanges: false,
        componentMocks: [
            DotWysiwygTinymceComponent,
            DotEditContentMonacoEditorControlComponent,
            DotLanguageVariableSelectorComponent
        ],
        providers: [
            mockProvider(DotWysiwygPluginService, {
                initializePlugins: jest.fn()
            }),
            mockProvider(DotWysiwygTinymceService, {
                getProps: () => of(mockSystemWideConfig)
            }),
            mockProvider(DotPropertiesService, {
                getKey: () => of(DEFAULT_IMAGE_URL_PATTERN)
            }),
            mockProvider(DotUploadFileService),
            provideHttpClient(),
            provideHttpClientTesting(),
            ConfirmationService
        ]
    });

    beforeEach(() => {
        // Needed for Dropdown PrimeNG to simulate a click and the overlay
        mockMatchMedia();
        mockScrollIntoView();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('should have the variable as id', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render TinyMCE as default editor', () => {
            expect(DEFAULT_EDITOR).toBe(AvailableEditor.TinyMCE);

            expect(spectator.query(DotWysiwygTinymceComponent)).toBeTruthy();
            expect(spectator.query(DotEditContentMonacoEditorControlComponent)).toBeNull();
        });

        it('should render editor selection dropdown', () => {
            expect(spectator.query(byTestId('editor-selector'))).toBeTruthy();

            // Open dropdown
            const dropdownTrigger = spectator.query('.p-dropdown-trigger');
            spectator.click(dropdownTrigger);
            spectator.detectChanges();

            expect(spectator.queryAll('.p-dropdown-item').length).toBe(EditorOptions.length);
        });

        it('should render editor selection dropdown and switch to Monaco editor when selected', () => {
            expect(spectator.query(DotWysiwygTinymceComponent)).toBeTruthy();
            expect(spectator.query(DotEditContentMonacoEditorControlComponent)).toBeNull();

            const onEditorChangeSpy = jest.spyOn(spectator.component, 'onEditorChange');

            // Open dropdown
            const dropdownTrigger = spectator.query('.p-dropdown-trigger');
            spectator.click(dropdownTrigger);
            spectator.detectChanges();

            const options = spectator.queryAll('.p-dropdown-item');
            spectator.click(options[1]);
            spectator.detectChanges();

            const content = spectator.component.$fieldContent();

            expect(content.length).toBe(0);
            expect(onEditorChangeSpy).toHaveBeenCalled();
            expect(spectator.query(DotWysiwygTinymceComponent)).toBeNull();
            expect(spectator.query(DotEditContentMonacoEditorControlComponent)).toBeTruthy();
        });

        it('should render language variable selector', () => {
            expect(spectator.query(DotLanguageVariableSelectorComponent)).toBeTruthy();
        });

        it('should change editor when dropdown value changes', () => {
            // Initial state
            expect(spectator.component.$displayedEditor()).toBe(AvailableEditor.TinyMCE);

            // Simulate editor change
            spectator.component.onEditorChange(AvailableEditor.Monaco);
            spectator.detectChanges();

            // Verify state change
            expect(spectator.component.$displayedEditor()).toBe(AvailableEditor.Monaco);
        });

        it('should call onSelectLanguageVariable when language variable is selected', () => {
            // Spy on component method
            const spy = jest.spyOn(spectator.component, 'onSelectLanguageVariable');

            // Get language variable selector component
            const languageVariableSelector = spectator.query(DotLanguageVariableSelectorComponent);

            // Trigger onSelectLanguageVariable event
            const testVariable = '${languageVariable}';
            languageVariableSelector.onSelectLanguageVariable.emit(testVariable);

            // Verify method called with correct parameter
            expect(spy).toHaveBeenCalledWith(testVariable);
        });
    });

    describe('disabledWYSIWYGChange', () => {
        it('should emit disabledWYSIWYGChange when switching from TinyMCE to Monaco editor', () => {
            // Arrange: Create a contentlet with empty content and no disabled settings
            const contentletMock = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: []
            } as DotCMSContentlet;

            const switchSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            switchSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            switchSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor (no content, so no confirmation dialog)
            switchSpectator.component.onEditorChange(AvailableEditor.Monaco);

            // Assert: disabledWYSIWYGChange should be emitted with field variable
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([WYSIWYG_MOCK.variable]);
            expect(switchSpectator.component.$displayedEditor()).toBe(AvailableEditor.Monaco);
        });

        it('should emit disabledWYSIWYGChange when switching from Monaco back to TinyMCE editor', () => {
            // Arrange: Create a contentlet with Monaco editor already enabled
            const contentletMock = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: [WYSIWYG_MOCK.variable]
            } as DotCMSContentlet;

            const switchBackSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([WYSIWYG_MOCK.variable])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            switchBackSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            switchBackSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to TinyMCE editor (should work without confirmation)
            switchBackSpectator.component.onEditorChange(AvailableEditor.TinyMCE);

            // Assert: disabledWYSIWYG should be cleared for TinyMCE
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([]);
            expect(switchBackSpectator.component.$displayedEditor()).toBe(AvailableEditor.TinyMCE);
        });

        it('should correctly initialize with Monaco editor when contentlet has preference set', () => {
            // Arrange: Contentlet with Monaco editor preference already set
            const contentletWithMonaco = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                disabledWYSIWYG: [WYSIWYG_MOCK.variable]
            } as DotCMSContentlet;

            const initSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([WYSIWYG_MOCK.variable])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: contentletWithMonaco
                    }
                }
            );
            initSpectator.detectChanges();

            // Assert: Should initialize with Monaco editor
            expect(initSpectator.component.$contentEditorUsed()).toBe(AvailableEditor.Monaco);
            expect(initSpectator.component.$displayedEditor()).toBe(AvailableEditor.Monaco);
            expect(initSpectator.component.$selectedEditorDropdown()).toBe(AvailableEditor.Monaco);
        });

        it('should preserve other field entries when updating current field editor preference', () => {
            // Arrange: Contentlet with existing entries for other fields
            const contentletMock = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: ['otherField', 'textAreaField@ToggleEditor']
            } as DotCMSContentlet;

            const preserveSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([
                                'otherField',
                                'textAreaField@ToggleEditor'
                            ])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            preserveSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            preserveSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor for current field
            preserveSpectator.component.onEditorChange(AvailableEditor.Monaco);

            // Assert: Should preserve other entries and add current field
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                'otherField',
                'textAreaField@ToggleEditor',
                WYSIWYG_MOCK.variable
            ]);
        });

        it('should handle smooth editor switching workflow without conflicts', () => {
            // Arrange: Start with clean contentlet
            const contentletEmpty = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: []
            } as DotCMSContentlet;

            const workflowSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-wysiwyg-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [WYSIWYG_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: WYSIWYG_MOCK,
                        contentlet: contentletEmpty
                    }
                }
            );
            workflowSpectator.detectChanges();

            const disabledWYSIWYGChangeSpy = jest.fn();
            workflowSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act 1: Switch to Monaco
            workflowSpectator.component.onEditorChange(AvailableEditor.Monaco);

            // Assert 1: Monaco active
            expect(workflowSpectator.component.$displayedEditor()).toBe(AvailableEditor.Monaco);
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([WYSIWYG_MOCK.variable]);

            // Act 2: Switch back to TinyMCE
            workflowSpectator.component.onEditorChange(AvailableEditor.TinyMCE);

            // Assert 2: TinyMCE active and preferences cleared
            expect(workflowSpectator.component.$displayedEditor()).toBe(AvailableEditor.TinyMCE);
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([]);
        });
    });
});
