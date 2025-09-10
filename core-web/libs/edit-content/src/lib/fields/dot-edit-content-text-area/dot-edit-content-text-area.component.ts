import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    input,
    model,
    output,
    viewChild
} from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import {
    AvailableEditorTextArea,
    TextAreaEditorOptions
} from './dot-edit-content-text-area.constants';

import { DISABLED_WYSIWYG_FIELD } from '../../models/disabledWYSIWYG.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';
import {
    getCurrentEditorFromDisabled,
    updateDisabledWYSIWYGOnEditorSwitch
} from '../shared/utils/field-editor-preferences.util';

/**
 * Text area component that provides plain text and code editing capabilities.
 * Supports two modes:
 * - Plain text editing through a standard textarea
 * - Code editing via Monaco editor with syntax highlighting
 *
 * Integrates with DotCMS form controls and content types, with automatic
 * editor mode detection and language variable support.
 */
@Component({
    selector: 'dot-edit-content-text-area',
    templateUrl: './dot-edit-content-text-area.component.html',
    styleUrls: ['./dot-edit-content-text-area.component.scss'],
    imports: [
        InputTextareaModule,
        ReactiveFormsModule,
        DotLanguageVariableSelectorComponent,
        DropdownModule,
        FormsModule,
        DotEditContentMonacoEditorControlComponent,
        DotCardFieldComponent,
        DotCardFieldLabelComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextAreaComponent extends BaseFieldComponent {
    /**
     * Control container for the form
     */
    private readonly controlContainer = inject(ControlContainer);

    /**
     * Reference to the textarea element
     */
    $textareaRef = viewChild<ElementRef<HTMLTextAreaElement>>('textarea');

    /**
     * Reference to the Monaco editor component
     */
    $monacoComponent = viewChild<DotEditContentMonacoEditorControlComponent>('monaco');

    /**
     * Input field DotCMSContentTypeField
     */
    $field = input.required<DotCMSContentTypeField | null>({ alias: 'field' });

    /**
     * Input contentlet DotCMSContentlet
     */
    $contentlet = input.required<DotCMSContentlet | null>({ alias: 'contentlet' });

    /**
     * Event emitted when disabledWYSIWYG changes.
     * Emits the updated disabledWYSIWYG array.
     */
    disabledWYSIWYGChange = output<string[]>();

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditorTextArea>();

    /**
     * The signal representing the currently displayed editor in the application.
     */
    $displayedEditor = model<AvailableEditorTextArea>(AvailableEditorTextArea.PlainText);

    /**
     * Computed property that determines the current editor based on disabledWYSIWYG settings
     */
    $contentEditorUsed = computed(() => {
        const field = this.$field();

        if (!field?.variable) {
            return AvailableEditorTextArea.PlainText; // Default editor
        }

        const disabledWYSIWYG = this.#getCurrentDisabledWYSIWYG();

        // Use disabledWYSIWYG setting
        const currentEditor = getCurrentEditorFromDisabled(field.variable, disabledWYSIWYG, true);

        if (
            currentEditor === AvailableEditorTextArea.Monaco ||
            currentEditor === AvailableEditorTextArea.PlainText
        ) {
            return currentEditor;
        }

        return null;
    });

    readonly textAreaEditorOptions = TextAreaEditorOptions;
    readonly editorTypes = AvailableEditorTextArea;

    constructor() {
        super();
        this.handleEditorChange(this.$contentEditorUsed);
    }

    /**
     * On select language variable
     * @param languageVariable - The parsed language variable string
     */
    onSelectLanguageVariable(languageVariable: string) {
        if (this.$displayedEditor() === AvailableEditorTextArea.PlainText) {
            const textarea = this.$textareaRef()?.nativeElement;
            if (!textarea) {
                return;
            }

            this.insertLanguageVariableInTextarea(textarea, languageVariable);
        } else if (this.$displayedEditor() === AvailableEditorTextArea.Monaco) {
            this.insertLanguageVariableInMonaco(languageVariable);
        }
    }

    /**
     * On editor change
     * @param newEditor - The new editor
     */
    onEditorChange(newEditor: AvailableEditorTextArea) {
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
            true // isTextAreaField
        );

        // Emit the change event
        this.disabledWYSIWYGChange.emit(updatedDisabledWYSIWYG);
        this.$displayedEditor.set(newEditor);
    }

    /**
     * Insert language variable at current cursor position in textarea
     * @param textarea - The textarea element
     * @param languageVariable - The parsed language variable string to insert
     * @private
     */
    private insertLanguageVariableInTextarea(
        textarea: HTMLTextAreaElement,
        languageVariable: string
    ): void {
        const control = this.$formControl();

        if (!control) {
            return;
        }

        const currentValue = control.value || '';
        const cursorPosition = textarea.selectionStart;
        const beforeCursor = currentValue.substring(0, cursorPosition);
        const afterCursor = currentValue.substring(cursorPosition);

        // Insert the language variable at cursor position
        const newValue = `${beforeCursor}${languageVariable}${afterCursor}`;

        // Update form control value
        control.setValue(newValue);

        // Update cursor position after the inserted text
        const newPosition = cursorPosition + languageVariable.length;
        textarea.focus();
        textarea.setSelectionRange(newPosition, newPosition);
    }

    /**
     * Insert language variable at current cursor position in Monaco editor
     * @param languageVariable - The parsed language variable string to insert
     * @private
     */
    private insertLanguageVariableInMonaco(languageVariable: string): void {
        const monaco = this.$monacoComponent();
        if (monaco) {
            monaco.insertContent(languageVariable);
        } else {
            console.warn('Monaco component is not available');
        }
    }

    /**
     * Handle editor change
     * @param newEditor - The new editor
     */
    readonly handleEditorChange = signalMethod<AvailableEditorTextArea>((newEditor) => {
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
