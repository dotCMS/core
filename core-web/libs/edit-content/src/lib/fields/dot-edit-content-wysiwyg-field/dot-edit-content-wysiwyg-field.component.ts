import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { JsonPipe } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    OnChanges,
    signal,
    SimpleChanges
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import {
    COMMENT_TINYMCE,
    DotWysiwygTinymceComponent
} from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    AvailableLanguageMonaco,
    DEFAULT_EDITOR,
    DEFAULT_MONACO_LANGUAGE,
    EditorOptions,
    HtmlTags,
    JsKeywords,
    MdSyntax,
    MonacoLanguageOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { CountOccurrences } from './dot-edit-content-wysiwyg-field.utils';

/**
 * Component representing a WYSIWYG (What You See Is What You Get) editor field for editing content in DotCMS.
 * Allows users to edit content using either the TinyMCE or Monaco editor, based on the content type and properties.
 */
@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    standalone: true,
    imports: [
        FormsModule,
        DropdownModule,
        DotWysiwygTinymceComponent,
        DotWysiwygMonacoComponent,
        MonacoEditorModule,
        JsonPipe,
        ConfirmDialogModule
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    host: {
        class: 'wysiwyg__wrapper'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentWYSIWYGFieldComponent implements AfterViewInit {
    #confirmationService = inject(ConfirmationService);
    #dotMessageService = inject(DotMessageService);
    /**
     * This variable represents a required content type field in DotCMS.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * A required input representing a DotCMS contentlet.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditor>();

    /**
     * A variable representing the currently selected language.
     */
    $selectedLanguageDropdown = model<string>(DEFAULT_EDITOR);

    $displayedEditor = signal<AvailableEditor>(DEFAULT_EDITOR);

    $defaultEditorUsed = computed(() => {
        const fieldValue = this.$field().variable;
        const contentlet = this.$contentlet();

        if (contentlet == null) {
            return DEFAULT_EDITOR;
        }

        const content = contentlet[fieldValue] as string;

        if (!content || content.trim() === COMMENT_TINYMCE) {
            return DEFAULT_EDITOR;
        }

        if (content.includes(COMMENT_TINYMCE)) {
            return AvailableEditor.TinyMCE;
        }

        return AvailableEditor.Monaco;
    });

    /**
     * A computed property that detects the programming language used in the editor.
     *
     * Determines the appropriate language based on the editor type and content.
     * Defaults to a specific language if the Monaco editor is not used, or if content
     * is absent. Analyzes the content to detect languages such as JavaScript and Markdown.
     *
     * Languages are detected based on the presence of specific keywords and syntax:
     * - JavaScript: Uses keywords like 'function', 'const ', 'let ', etc.
     * - Markdown: Based on the frequency of markdown syntax elements like '# ', '```', etc.
     * - Plain text
     *
     */
    $detectLanguage = computed(() => {
        if (this.$defaultEditorUsed() !== AvailableEditor.Monaco) {
            return DEFAULT_MONACO_LANGUAGE;
        }

        const fieldValue = this.$field().variable;
        const contentlet = this.$contentlet();

        if (contentlet == null) {
            return DEFAULT_MONACO_LANGUAGE;
        }

        const content = contentlet[fieldValue] as string;

        if (!content) {
            return DEFAULT_MONACO_LANGUAGE;
        }

        if (JsKeywords.some((keyword) => content.includes(keyword))) {
            return AvailableLanguageMonaco.Javascript;
        }

        if (HtmlTags.some((tag) => content.indexOf(tag) !== -1)) {
            return AvailableLanguageMonaco.Html;
        }

        const mdScore = MdSyntax.reduce(
            (score, syntax) => score + CountOccurrences(content, syntax),
            0
        );
        if (mdScore > 2) {
            return AvailableLanguageMonaco.Markdown;
        }

        return AvailableLanguageMonaco.PlainText;
    });

    readonly editorTypes = AvailableEditor;
    readonly editorOptions = EditorOptions;
    readonly monacoLanguagesOptions = MonacoLanguageOptions;

    ngAfterViewInit(): void {
        // Assign the selected editor value
        this.$selectedEditorDropdown.set(this.$defaultEditorUsed());
        // Editor showed
        this.$displayedEditor.set(this.$defaultEditorUsed());
        // Assign the selected language
        this.$selectedLanguageDropdown.set(this.$detectLanguage());
    }

    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();

        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'edit.content.wysiwyg.confirm.switch-editor.header'
            ),
            message: this.#dotMessageService.get(
                'edit.content.wysiwyg.confirm.switch-editor.message'
            ),
            rejectButtonStyleClass: 'p-button-text',
            acceptIcon: 'none',
            rejectIcon: 'none',
            accept: () => {
                this.$displayedEditor.set(newEditor);
            },
            reject: () => {
                this.$selectedEditorDropdown.set(currentDisplayedEditor);
            }
        });
    }
}
