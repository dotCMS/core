import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';

import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    COMMENT_TINYMCE,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { shouldUseDefaultEditor } from './dot-edit-content-wysiwyg-field.utils';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';

/**
 * Component representing a WYSIWYG (What You See Is What You Get) editor field for editing content in DotCMS.
 * Allows users to edit content using either the TinyMCE or Monaco editor, based on the content type and properties.
 */
@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    imports: [
        FormsModule,
        DropdownModule,
        DotWysiwygTinymceComponent,
        DotEditContentMonacoEditorControlComponent,
        MonacoEditorModule,
        ConfirmDialogModule,
        DotLanguageVariableSelectorComponent
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    host: {
        class: 'dot-wysiwyg__wrapper'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentWYSIWYGFieldComponent implements AfterViewInit {
    /**
     * Signal to get the TinyMCE component.
     */
    $tinyMCEComponent = viewChild(DotWysiwygTinymceComponent);

    /**
     * Signal to get the Monaco component.
     */
    $monacoComponent = viewChild(DotEditContentMonacoEditorControlComponent);

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
     * The signal representing the currently displayed editor in the application.
     */
    $displayedEditor = model<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * Computed property that determines the default editor used for a contentlet.
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
     */
    $fieldContent = computed<string>(() => {
        const fieldValue = this.$field().variable;
        const contentlet = this.$contentlet();

        if (contentlet == null) {
            return '';
        }

        return contentlet[fieldValue] as string;
    });

    readonly editorTypes = AvailableEditor;
    readonly editorOptions = EditorOptions;

    ngAfterViewInit(): void {
        // Assign the selected editor value
        this.$selectedEditorDropdown.set(this.$contentEditorUsed());
        // Editor showed
        this.$displayedEditor.set(this.$contentEditorUsed());
    }

    /**
     * Handles the editor change event by prompting the user for confirmation.
     */
    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();
        const content = this.$fieldContent();

        if (content?.length > 0 && this.$displayedEditor() !== AvailableEditor.TinyMCE) {
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

    /**
     * Handles the selection of a language variable from the autocomplete.
     */
    onSelectLanguageVariable(languageVariable: string) {
        if (this.$displayedEditor() === AvailableEditor.TinyMCE) {
            const tinyMCE = this.$tinyMCEComponent();
            if (tinyMCE) {
                tinyMCE.insertContent(languageVariable);
            } else {
                console.warn('TinyMCE component is not available');
            }
        } else if (this.$displayedEditor() === AvailableEditor.Monaco) {
            const monaco = this.$monacoComponent();
            if (monaco) {
                monaco.insertContent(languageVariable);
            } else {
                console.warn('Monaco component is not available');
            }
        }
    }
}
