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
import { mockMatchMedia, monacoMock } from '@dotcms/utils-testing';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import { DotWysiwygTinymceService } from './components/dot-wysiwyg-tinymce/service/dot-wysiwyg-tinymce.service';
import { DotEditContentWYSIWYGFieldComponent } from './dot-edit-content-wysiwyg-field.component';
import {
    AvailableEditor,
    AvailableLanguageMonaco,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';
import { DEFAULT_IMAGE_URL_PATTERN } from './dot-wysiwyg-plugin/utils/editor.utils';
import {
    WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
    WYSIWYG_MOCK,
    WYSIWYG_FIELD_CONTENTLET_MOCK_WITH_WYSIWYG_CONTENT,
    WYSIWYG_VARIABLE_NAME
} from './mocks/dot-edit-content-wysiwyg-field.mock';

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
        imports: [DropdownModule, NoopAnimationsModule, FormsModule, ConfirmDialogModule],
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
            expect(spectator.query(DotWysiwygMonacoComponent)).toBeNull();
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
            expect(spectator.query(DotWysiwygMonacoComponent)).toBeNull();

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
            expect(spectator.query(DotWysiwygMonacoComponent)).toBeTruthy();
        });
    });

    describe('With Monaco Editor', () => {
        beforeEach(() => {
            spectator.component.$selectedEditorDropdown.set(AvailableEditor.Monaco);
            spectator.detectChanges();
        });

        it('should has a dropdown for language selection', () => {
            expect(spectator.query(byTestId('language-selector'))).toBeTruthy();
            expect(spectator.query(byTestId('editor-selector'))).toBeTruthy();
        });

        it('should selected `javascript` as selected language', () => {
            spectator = createComponent({
                props: {
                    field: WYSIWYG_MOCK,
                    contentlet: {
                        ...WYSIWYG_FIELD_CONTENTLET_MOCK_WITH_WYSIWYG_CONTENT,
                        [WYSIWYG_VARIABLE_NAME]: 'const a = 5;'
                    }
                } as unknown,
                detectChanges: false
            });
            spectator.detectChanges();

            expect(spectator.component.$contentEditorUsed()).toBe(AvailableEditor.Monaco);
            expect(spectator.component.$contentLanguageUsed()).toBe(
                AvailableLanguageMonaco.Javascript
            );
        });

        it('should selected `markdown` as selected language', () => {
            spectator = createComponent({
                props: {
                    field: WYSIWYG_MOCK,
                    contentlet: {
                        ...WYSIWYG_FIELD_CONTENTLET_MOCK_WITH_WYSIWYG_CONTENT,
                        [WYSIWYG_VARIABLE_NAME]: `# Main title
                        ## Level 2 title`
                    }
                } as unknown,
                detectChanges: false
            });
            spectator.detectChanges();

            expect(spectator.component.$contentEditorUsed()).toBe(AvailableEditor.Monaco);
            expect(spectator.component.$contentLanguageUsed()).toBe(
                AvailableLanguageMonaco.Markdown
            );
        });

        it('should selected `html` as selected language', () => {
            spectator = createComponent({
                props: {
                    field: WYSIWYG_MOCK,
                    contentlet: {
                        ...WYSIWYG_FIELD_CONTENTLET_MOCK_WITH_WYSIWYG_CONTENT,
                        [WYSIWYG_VARIABLE_NAME]: `<h1>Title</h1> <p>content</p>`
                    }
                } as unknown,
                detectChanges: false
            });
            spectator.detectChanges();

            expect(spectator.component.$contentEditorUsed()).toBe(AvailableEditor.Monaco);
            expect(spectator.component.$contentLanguageUsed()).toBe(AvailableLanguageMonaco.Html);
        });
    });
});
