import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    input,
    model,
    viewChild
} from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';

import {
    AvailableEditorTextArea,
    TextAreaEditorOptions
} from './dot-edit-content-text-area.constants';
import { detectEditorType } from './utils/content-type-detector.util';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import {
    addMonacoMarker,
    removeMonacoMarker
} from '../../shared/dot-edit-content-monaco-editor-control/monaco-marker.util';

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
        DotEditContentMonacoEditorControlComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextAreaComponent implements AfterViewInit {
    /**
     * Control container for the form
     */
    private readonly controlContainer = inject(ControlContainer);

    /**
     * Reference to the textarea element
     */
    private readonly textareaRef = viewChild<ElementRef<HTMLTextAreaElement>>('textarea');

    /**
     * Reference to the Monaco editor component
     */
    private readonly $monacoComponent =
        viewChild<DotEditContentMonacoEditorControlComponent>('monaco');

    /**
     * Input field DotCMSContentTypeField
     */
    $field = input.required<DotCMSContentTypeField | null>({ alias: 'field' });

    /**
     * Computed property that returns the current value of the field.
     */
    $currentValue = computed(() => {
        const { variable } = this.$field();
        const control = this.controlContainer.control?.get(variable);

        return control?.value ?? '';
    });

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditorTextArea>();

    /**
     * The signal representing the currently displayed editor in the application.
     */
    $displayedEditor = model<AvailableEditorTextArea>(AvailableEditorTextArea.PlainText);

    /**
     * Computed property that determines the default editor based on content analysis.
     * Uses content-type-detector utility to determine if content needs Monaco editor.
     */
    $contentEditorUsed = computed(() => {
        const content = this.$currentValue();

        return detectEditorType(content);
    });

    readonly textAreaEditorOptions = TextAreaEditorOptions;
    readonly editorTypes = AvailableEditorTextArea;

    ngAfterViewInit(): void {
        const currentEditor = this.$contentEditorUsed();
        // Assign the selected editor value
        this.$selectedEditorDropdown.set(currentEditor);
        // Editor showed
        this.$displayedEditor.set(currentEditor);
    }

    /**
     * On select language variable
     * @param languageVariable - The parsed language variable string
     */
    onSelectLanguageVariable(languageVariable: string) {
        if (this.$displayedEditor() === AvailableEditorTextArea.PlainText) {
            const textarea = this.textareaRef()?.nativeElement;
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
        const control = this.controlContainer.control?.get(this.$field()?.variable);

        if (newEditor === AvailableEditorTextArea.PlainText) {
            control?.setValue(removeMonacoMarker(this.$currentValue()));
        } else {
            control?.setValue(addMonacoMarker(this.$currentValue()));
        }

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
        const control = this.controlContainer.control?.get(this.$field().variable);

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
}
