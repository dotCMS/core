import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    forwardRef,
    inject,
    input,
    model,
    output,
    viewChild
} from '@angular/core';
import {
    ControlContainer,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';

import { DISABLED_WYSIWYG_FIELD } from '../../models/disabledWYSIWYG.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';
import {
    getCurrentEditorFromDisabled,
    updateDisabledWYSIWYGOnEditorSwitch
} from '../shared/utils/field-editor-preferences.util';

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
        DotLanguageVariableSelectorComponent,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentWYSIWYGFieldComponent)
        }
    ]
})
export class DotEditContentWYSIWYGFieldComponent extends BaseFieldComponent {
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
     * Event emitted when disabledWYSIWYG changes.
     * Emits the updated disabledWYSIWYG array.
     */
    disabledWYSIWYGChange = output<string[]>();

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditor>();

    /**
     * The signal representing the currently displayed editor in the application.
     */
    $displayedEditor = model<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * Computed property that determines the current editor based on disabledWYSIWYG settings
     * with fallback to content analysis when no entry exists.
     */
    $contentEditorUsed = computed(() => {
        const field = this.$field();

        if (!field?.variable) {
            return DEFAULT_EDITOR;
        }

        const disabledWYSIWYG = this.#getCurrentDisabledWYSIWYG();

        // Use disabledWYSIWYG setting
        const currentEditor = getCurrentEditorFromDisabled(field.variable, disabledWYSIWYG, false);

        if (currentEditor === AvailableEditor.Monaco || currentEditor === AvailableEditor.TinyMCE) {
            return currentEditor;
        }

        return null;
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

    constructor() {
        super();
        this.handleEditorChange(this.$contentEditorUsed);
    }

    /**
     * Handles the editor change event by prompting the user for confirmation.
     */
    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();
        const content = this.$fieldContent();

        const updateEditorAndDisabledWYSIWYG = () => {
            const field = this.$field();

            if (!field?.variable) {
                throw new Error('Field variable is not available');
            }

            // Update disabledWYSIWYG in the contentlet
            const currentDisabledWYSIWYG = this.#getCurrentDisabledWYSIWYG();
            const updatedDisabledWYSIWYG = updateDisabledWYSIWYGOnEditorSwitch(
                field.variable,
                newEditor,
                currentDisabledWYSIWYG,
                false // isTextAreaField
            );

            // Emit the change event
            this.disabledWYSIWYGChange.emit(updatedDisabledWYSIWYG);
            this.$displayedEditor.set(newEditor);
        };

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
                accept: updateEditorAndDisabledWYSIWYG,
                reject: () => {
                    this.$selectedEditorDropdown.set(currentDisplayedEditor);
                }
            });
        } else {
            updateEditorAndDisabledWYSIWYG();
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

    /**
     * Handle editor change
     * @param newEditor - The new editor
     */
    readonly handleEditorChange = signalMethod<AvailableEditor>((newEditor) => {
        if (!newEditor) {
            return;
        }

        this.$selectedEditorDropdown.set(newEditor);
        this.$displayedEditor.set(newEditor);
    });

    /**
     * Get the current disabledWYSIWYG value from the form control
     * @returns The current disabledWYSIWYG value
     */
    #getCurrentDisabledWYSIWYG() {
        return this.disabledWYSIWYGField?.value ?? [];
    }

    /**
     * Get the disabledWYSIWYG field from the form control
     * @returns The disabledWYSIWYG field
     */
    get disabledWYSIWYGField() {
        return this.controlContainer.control?.get(DISABLED_WYSIWYG_FIELD);
    }

    writeValue(_: unknown): void {
        // noop
    }
}
