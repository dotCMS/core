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
import { WYSIWYG_MOCK } from './mocks/dot-edit-content-wysiwyg-field.mock';

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
                field: WYSIWYG_MOCK
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

            const content = spectator.component.$currentValue();

            expect(content.length).toBe(0);
            expect(onEditorChangeSpy).toHaveBeenCalled();
            expect(spectator.query(DotWysiwygTinymceComponent)).toBeNull();
            expect(spectator.query(DotEditContentMonacoEditorControlComponent)).toBeTruthy();
        });

        it('should render language variable selector', () => {
            expect(spectator.query(DotLanguageVariableSelectorComponent)).toBeTruthy();
        });
    });
});
