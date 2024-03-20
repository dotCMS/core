import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { EditorComponent, EditorModule } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';

import {
    ControlContainer,
    FormGroupDirective,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';

import { DotWYSIWYGFieldComponent } from './dot-wysiwyg-field.component';

import { WYSIWYG_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

const ALL_PLUGINS =
    'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template';
const ALL_TOOLBAR_ITEMS =
    'paste print textpattern | insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image hr | preview | validation media | forecolor dotimageclipboard backcolor emoticons';

describe('DotWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotWYSIWYGFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotWYSIWYGFieldComponent,
        imports: [EditorModule, FormsModule, ReactiveFormsModule],
        declarations: [MockComponent(EditorComponent)],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            }
        });
    });

    it('should instance WYSIWYG editor and set the correct plugins and toolbar items', () => {
        const editor = spectator.query(EditorComponent);
        expect(editor).toBeTruthy();
        expect(editor.plugins).toEqual(ALL_PLUGINS);
        expect(editor.toolbar).toEqual(ALL_TOOLBAR_ITEMS);
    });
});
