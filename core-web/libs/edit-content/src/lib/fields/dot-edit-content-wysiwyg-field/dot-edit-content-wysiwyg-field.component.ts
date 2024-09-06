import { ChangeDetectionStrategy, Component, model, input } from '@angular/core';
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
    imports: [FormsModule, DropdownModule, DotWysiwygTinymceComponent, DotWysiwygMonacoComponent],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    host: {
        class: 'wysiwyg__wrapper'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentWYSIWYGFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * A variable representing the editor selected by the user.
     */
    selectedEditor = model<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * A variable representing the currently selected language.
     */
    selectedLanguage = model<string>(DEFAULT_MONACO_LANGUAGE);

    protected editorTypes = AvailableEditor;
    protected editorOptions = EditorOptions;
    protected monacoLanguagesOptions = MonacoLanguageOptions;
}
