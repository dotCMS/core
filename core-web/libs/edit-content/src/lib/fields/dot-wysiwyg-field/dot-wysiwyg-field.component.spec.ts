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

import { DotWYSIWYGFieldComponent } from './dot-wysiwyg-field.component';
import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

import { WYSIWYG_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

const ALL_PLUGINS =
    'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template';
const ALL_TOOLBAR_ITEMS =
    'paste print textpattern | insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | dotAddImage | link hr | preview | validation media | forecolor backcolor emoticons';

describe('DotWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotWYSIWYGFieldComponent>;
    let dotWysiwygPluginService: DotWysiwygPluginService;

    const createComponent = createComponentFactory({
        component: DotWYSIWYGFieldComponent,
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
