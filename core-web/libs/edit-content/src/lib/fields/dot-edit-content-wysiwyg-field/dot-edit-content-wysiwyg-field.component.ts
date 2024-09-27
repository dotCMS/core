import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    AvailableLanguageMonaco,
    COMMENT_TINYMCE,
    DEFAULT_EDITOR,
    DEFAULT_MONACO_LANGUAGE,
    EditorOptions,
    HtmlTags,
    JsKeywords,
    MdSyntax,
    MonacoLanguageOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { CountOccurrences, shouldUseDefaultEditor } from './dot-edit-content-wysiwyg-field.utils';

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

    /**
     * The signal representing the currently displayed editor in the application.
     * It holds an `AvailableEditor` type, which determines which editor is actively shown to the user.
     * The initial value is set to `DEFAULT_EDITOR`.
     */
    $displayedEditor = signal<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * Computed property that determines the default editor used for a contentlet.
     *
     * It inspects the value of a specific field from the contentlet object and returns an
     * appropriate editor based on the content of the field.
     *
     */
    $contentEditorUsed = computed(() => {
        const content = this.$fieldContent();

        if (shouldUseDefaultEditor(content)) {
            return DEFAULT_EDITOR;
        }

        if (content.includes(COMMENT_TINYMCE)) {
            return AvailableEditor.TinyMCE;
        }

        return AvailableEditor.Monaco;
    });

    /**
     * Computed property that returns the content corresponding to the specified field value.
     * It retrieves the `variable` property from `$field` and uses it to access the corresponding
     * value in the `$contentlet`. If the `$contentlet` is null, it returns an empty string.
     *
     * @type {string}
     */
    $fieldContent = computed<string>(() => {
        const fieldValue = this.$field().variable;
        const contentlet = this.$contentlet();

        if (contentlet == null) {
            return '';
        }

        return contentlet[fieldValue] as string;
    });

    /**
     * A computed property that determines the appropriate language mode for the content editor.
     * This is based on the content type present in the editor.
     */
    $contentLanguageUsed = computed(() => {
        if (this.$contentEditorUsed() !== AvailableEditor.Monaco) {
            return DEFAULT_MONACO_LANGUAGE;
        }

        const content = this.$fieldContent();

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
        this.$selectedEditorDropdown.set(this.$contentEditorUsed());
        // Editor showed
        this.$displayedEditor.set(this.$contentEditorUsed());
        // Assign the selected language
        this.$selectedLanguageDropdown.set(this.$contentLanguageUsed());
    }

    /**
     * Handles the editor change event by prompting the user for confirmation.
     * If accepted, the new editor is set. If rejected, the displayed editor remains unchanged.
     *
     * @param {AvailableEditor} newEditor - The editor to switch to.
     * @return {void} - This method does not return a value.
     */
    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();
        const content = this.$fieldContent();

        if (content.length > 0 && this.$displayedEditor() !== AvailableEditor.TinyMCE) {
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
        } else {
            this.$displayedEditor.set(newEditor);
        }
    }
}
