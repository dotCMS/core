import { expect } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import {
    DotLanguagesService,
    DotLanguageVariableEntry,
    DotPropertiesService,
    DotUploadFileService
} from '@dotcms/data-access';
import { DotMessagePipe, mockMatchMedia, monacoMock } from '@dotcms/utils-testing';

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
import {
    WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT,
    WYSIWYG_MOCK
} from './mocks/dot-edit-content-wysiwyg-field.mock';

import { createFormGroupDirectiveMock } from '../../utils/mocks';

const mockLanguageVariables: Record<string, DotLanguageVariableEntry> = {
    'ai-text-area-key': {
        'en-us': {
            identifier: '034a07f0f308db12d55fa74bb3b265f0',
            value: 'AI text area value'
        },
        'es-es': null,
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Freddy': {
        'en-us': null,
        'es-es': {
            identifier: '175d27eb-9e2c-4fdc-9c4a-0e7d88ce4e87',
            value: 'Freddy'
        },
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Landing-Pages': {
        'en-us': {
            identifier: '06e1f11b-410a-428b-947c-ed60dcc8420d',
            value: 'Landing Pages'
        },
        'es-es': {
            identifier: '1547f21d-c357-4524-afb0-b728fe3217db',
            value: 'Landing Pages'
        },
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Personas': {
        'en-us': {
            identifier: '1102be5608453fb28485c5f1060f5be3',
            value: 'Personas'
        },
        'es-es': null,
        es_pa: null
    }
};

const mockScrollIntoView = () => {
    Element.prototype.scrollIntoView = jest.fn();
};

const mockSystemWideConfig = { systemWideOption: 'value' };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotEditContentWYSIWYGFieldComponent>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

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
            mockProvider(DotLanguagesService),
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

        dotLanguagesService = spectator.inject(DotLanguagesService);

        dotLanguagesService.getLanguageVariables.mockReturnValue(of(mockLanguageVariables));

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

        it('should render language variable selector', () => {
            expect(spectator.query(byTestId('language-variable-selector'))).toBeTruthy();
        });
    });
});
