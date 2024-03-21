import { expect } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator';
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

const ALL_PLUGINS =
    'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template';
const ALL_TOOLBAR_ITEMS =
    'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr';

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
            }
        });

        dotWysiwygPluginService = spectator.inject(DotWysiwygPluginService, true);
    });

    it('should instance WYSIWYG editor and set the correct plugins and toolbar items', () => {
        const editor = spectator.query(EditorComponent);
        expect(editor).toBeTruthy();
        expect(editor.plugins).toEqual(ALL_PLUGINS);
        expect(editor.toolbar).toEqual(ALL_TOOLBAR_ITEMS);
        expect(editor.init).toEqual({
            menubar: false,
            image_caption: true,
            image_advtab: true,
            contextmenu: 'align link image',
            setup: expect.any(Function)
        });
    });

    it('should initialize Plugins when the setup method is called', () => {
        const spy = jest.spyOn(dotWysiwygPluginService, 'initializePlugins');
        const editor = spectator.query(EditorComponent);
        const mockEditor = {} as Editor;
        editor.init.setup(mockEditor);
        expect(spy).toHaveBeenCalledWith(mockEditor);
    });
});
