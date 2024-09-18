import { Spectator, createComponentFactory, byTestId, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotPropertiesService, DotUploadFileService } from '@dotcms/data-access';
import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
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

import { createFormGroupDirectiveMock, WYSIWYG_MOCK } from '../../utils/mocks';

const mockScrollIntoView = () => {
    Element.prototype.scrollIntoView = jest.fn();
};

const mockSystemWideConfig = { systemWideOption: 'value' };

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotEditContentWYSIWYGFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentWYSIWYGFieldComponent,
        imports: [DropdownModule, NoopAnimationsModule, FormsModule],
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
            provideHttpClientTesting()
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

            spectator.component.$selectedEditor.set(AvailableEditor.Monaco);
            spectator.detectChanges();

            expect(spectator.query(DotWysiwygTinymceComponent)).toBeNull();
            expect(spectator.query(DotWysiwygMonacoComponent)).toBeTruthy();
        });
    });

    // describe('TinyMCE Editor', () => {});
    describe('With Monaco Editor', () => {
        beforeEach(() => {
            spectator.component.$selectedEditor.set(AvailableEditor.Monaco);
            spectator.detectChanges();
        });

        it('should has a dropdown for language selection', () => {
            expect(spectator.query(byTestId('language-selector'))).toBeTruthy();
            expect(spectator.query(byTestId('editor-selector'))).toBeTruthy();
        });
    });
});
