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
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';

import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import {
    addMonacoMarker,
    hasMonacoMarker,
    removeMonacoMarker
} from '../../shared/dot-edit-content-monaco-editor-control/monaco-marker.util';

/**
 * Component representing a WYSIWYG (What You See Is What You Get) editor field for editing content in DotCMS.
 * Allows users to edit content using either the TinyMCE or Monaco editor, based on the content type and properties.
 */
@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    imports: [
        FormsModule,
        ReactiveFormsModule,
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
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentWYSIWYGFieldComponent implements AfterViewInit {
    /**
     * Control container for the form
     */
    private readonly controlContainer = inject(ControlContainer);

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
     * Input field DotCMSContentTypeField
     */
    $field = input<DotCMSContentTypeField | null>(null, {
        alias: 'field'
    });

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditor>();

    /**
     * The signal representing the currently displayed editor in the application.
     */
    $displayedEditor = model<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * Computed property that returns the current value of the field.
     */
    $currentValue = computed(() => {
        const { variable } = this.$field();
        const control = this.controlContainer.control?.get(variable);

        return control?.value ?? '';
    });

    /**
     * Computed property that determines the default editor used for a contentlet.
     * Uses Monaco marker for centralized editor selection logic.
     */
    $contentEditorUsed = computed(() => {
        const content = this.$currentValue();

        if (hasMonacoMarker(content)) {
            return AvailableEditor.Monaco;
        }

        return AvailableEditor.TinyMCE;
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
     * Adds or removes Monaco markers as needed when switching editors.
     */
    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();
        const content = this.$currentValue();

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
                    this.updateContentMarker(newEditor);
                    this.$displayedEditor.set(newEditor);
                },
                reject: () => {
                    this.$selectedEditorDropdown.set(currentDisplayedEditor);
                }
            });
        } else {
            this.updateContentMarker(newEditor);
            this.$displayedEditor.set(newEditor);
        }
    }

    /**
     * Updates the content with appropriate markers based on the selected editor.
     * @private
     */
    private updateContentMarker(newEditor: AvailableEditor): void {
        const fieldVariable = this.$field().variable;
        const currentContent = this.$currentValue();
        const control = this.controlContainer.control?.get(fieldVariable);

        if (!control) {
            return;
        }

        let updatedContent: string;

        if (newEditor === AvailableEditor.Monaco) {
            // Add Monaco marker for Monaco editor
            updatedContent = addMonacoMarker(currentContent);
        } else {
            // Remove Monaco marker for TinyMCE editor
            updatedContent = removeMonacoMarker(currentContent);
        }

        // Update form control value
        control.setValue(updatedContent);
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
