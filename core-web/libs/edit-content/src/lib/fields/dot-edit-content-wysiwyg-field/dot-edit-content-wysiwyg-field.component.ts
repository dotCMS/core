import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    DEFAULT_EDITOR,
    DEFAULT_MONACO_LANGUAGE,
    EditorOptions,
    MonacoLanguageOptions
} from './dot-edit-content-wysiwyg-field.constant';

@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    standalone: true,
    imports: [
        FormsModule,
        DropdownModule,
        DotWysiwygTinymceComponent,
        DotWysiwygMonacoComponent,
        MonacoEditorModule
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    host: {
        class: 'wysiwyg__wrapper'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentWYSIWYGFieldComponent {
    /**
     * This variable represents a required content type field in DotCMS.
     */
    $field = input<DotCMSContentTypeField>({} as DotCMSContentTypeField, { alias: 'field' });

    /**
     * A variable representing the editor selected by the user.
     */
    $selectedEditor = signal<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * A variable representing the currently selected language.
     */
    $selectedLanguage = signal<string>(DEFAULT_MONACO_LANGUAGE);

    readonly editorTypes = AvailableEditor;
    readonly editorOptions = EditorOptions;
    readonly monacoLanguagesOptions = MonacoLanguageOptions;
}
