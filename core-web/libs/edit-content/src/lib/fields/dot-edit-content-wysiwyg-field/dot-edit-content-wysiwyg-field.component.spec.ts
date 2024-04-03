import { expect } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { EditorComponent, EditorModule } from '@tinymce/tinymce-angular';
import { MockComponent, MockService } from 'ng-mocks';
import { Editor } from 'tinymce';

import {
    ControlContainer,
    FormGroupDirective,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotUploadFileService } from '@dotcms/data-access';

import { DotEditContentWYSIWYGFieldComponent } from './dot-edit-content-wysiwyg-field.component';
import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

import { WYSIWYG_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

const DEFAULT_CONFIG = {
    menubar: false,
    image_caption: true,
    image_advtab: true,
    contextmenu: 'align link image',
    toolbar1:
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr',
    plugins:
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template'
};

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotEditContentWYSIWYGFieldComponent>;
    let dotWysiwygPluginService: DotWysiwygPluginService;

    const createComponent = createComponentFactory({
        component: DotEditContentWYSIWYGFieldComponent,
        imports: [EditorModule, FormsModule, ReactiveFormsModule],
        declarations: [MockComponent(EditorComponent)],
        componentViewProviders: [
            {
                provide: DotWysiwygPluginService,
                useValue: {
                    initializePlugins: jest.fn()
                }
            },
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [
            FormGroupDirective,
            DialogService,
            {
                provide: DotUploadFileService,
                useValue: MockService(DotUploadFileService)
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            },
            detectChanges: false
        });

        dotWysiwygPluginService = spectator.inject(DotWysiwygPluginService, true);
    });

    it('should instance WYSIWYG editor and set the correct configuration', () => {
        spectator.detectChanges();
        const editor = spectator.query(EditorComponent);
        expect(editor.init).toEqual({
            ...DEFAULT_CONFIG,
            setup: expect.any(Function)
        });
    });

    it('should initialize Plugins when the setup method is called', () => {
        spectator.detectChanges();
        const spy = jest.spyOn(dotWysiwygPluginService, 'initializePlugins');
        const editor = spectator.query(EditorComponent);
        const mockEditor = {} as Editor;
        editor.init.setup(mockEditor);
        expect(spy).toHaveBeenCalledWith(mockEditor);
    });

    describe('variables', () => {
        it('should overwrite the editor configuration with the field variables', () => {
            const fieldVariables = [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                    fieldId: '1',
                    id: '1',
                    key: 'tinymceprops',
                    value: '{ "toolbar1": "undo redo"}'
                }
            ];

            spectator.setInput('field', {
                ...WYSIWYG_MOCK,
                fieldVariables
            });

            const editor = spectator.query(EditorComponent);
            expect(editor.init).toEqual({
                ...DEFAULT_CONFIG,
                toolbar1: 'undo redo',
                setup: expect.any(Function)
            });
        });

        it('should not configure theme property', () => {
            const fieldVariables = [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                    fieldId: '1',
                    id: '1',
                    key: 'tinymceprops',
                    value: '{theme: "modern"}'
                }
            ];

            spectator.setInput('field', {
                ...WYSIWYG_MOCK,
                fieldVariables
            });

            const editor = spectator.query(EditorComponent);
            expect(editor.init).toEqual({
                ...DEFAULT_CONFIG,
                setup: expect.any(Function)
            });
        });
    });
});
