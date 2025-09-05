import { expect } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotPropertiesService, DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
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
import { createFormGroupDirectiveMock } from '../../utils/mocks';

const mockScrollIntoView = () => {
    Element.prototype.scrollIntoView = jest.fn();
};

const mockSystemWideConfig = { systemWideOption: 'value' };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotEditContentWYSIWYGFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentWYSIWYGFieldComponent,
        imports: [
            DropdownModule,
            NoopAnimationsModule,
            FormsModule,
            ConfirmDialogModule,
            DotMessagePipe
        ],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            },
            {
                provide: DotWysiwygPluginService,
                useValue: {
                    initializePlugins: jest.fn()
                }
            },
            mockProvider(DotWysiwygTinymceService, {
                getProps: () => of(mockSystemWideConfig)
            }),
            mockProvider(DotPropertiesService, {
                getKey: () => of(DEFAULT_IMAGE_URL_PATTERN)
            })
        ],
        providers: [
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
        // end
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK,
                contentlet: WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT
            } as unknown,
            detectChanges: false
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('UI', () => {
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
    });

    describe('disabledWYSIWYGChange', () => {
        it('should emit disabledWYSIWYGChange when switching from TinyMCE to Monaco editor', () => {
            // Arrange: Create a contentlet with empty content and no disabled settings
            const contentletMock = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: []
            } as DotCMSContentlet;

            const switchSpectator = createComponent({
                props: {
                    contentlet: contentletMock,
                    field: WYSIWYG_MOCK
                } as unknown
            });
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

            const switchBackSpectator = createComponent({
                props: {
                    contentlet: contentletMock,
                    field: WYSIWYG_MOCK
                } as unknown
            });
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

            const initSpectator = createComponent({
                props: {
                    contentlet: contentletWithMonaco,
                    field: WYSIWYG_MOCK
                } as unknown
            });
            initSpectator.component.disabledWYSIWYGField.setValue([WYSIWYG_MOCK.variable]);
            initSpectator.detectChanges();

            // Assert: Should initialize with Monaco editor
            expect(initSpectator.component.$contentEditorUsed()).toBe(AvailableEditor.TinyMCE);
            expect(initSpectator.component.$displayedEditor()).toBe(AvailableEditor.TinyMCE);
            expect(initSpectator.component.$selectedEditorDropdown()).toBe(AvailableEditor.TinyMCE);
        });

        it('should preserve other field entries when updating current field editor preference', () => {
            // Arrange: Contentlet with existing entries for other fields
            const contentletMock = {
                ...WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
                [WYSIWYG_MOCK.variable]: '',
                disabledWYSIWYG: ['otherField', 'textAreaField@ToggleEditor']
            } as DotCMSContentlet;

            const preserveSpectator = createComponent({
                props: {
                    contentlet: contentletMock,
                    field: WYSIWYG_MOCK
                } as unknown
            });
            preserveSpectator.component.disabledWYSIWYGField.setValue([
                'otherField',
                'textAreaField@ToggleEditor'
            ]);
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

            const workflowSpectator = createComponent({
                props: {
                    contentlet: contentletEmpty,
                    field: WYSIWYG_MOCK
                } as unknown
            });
            workflowSpectator.component.disabledWYSIWYGField.setValue([]);
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
