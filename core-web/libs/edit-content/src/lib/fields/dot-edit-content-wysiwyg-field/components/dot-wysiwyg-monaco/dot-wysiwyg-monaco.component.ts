import { MonacoEditorComponent, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnDestroy,
    viewChild
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { PaginatorModule } from 'primeng/paginator';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getFieldVariablesParsed, stringToJson } from '../../../../utils/functions.util';
import {
    COMMENT_TINYMCE,
    DEFAULT_MONACO_LANGUAGE,
    DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG
} from '../../dot-edit-content-wysiwyg-field.constant';

/**
 * DotWysiwygMonacoComponent is an Angular component utilizing Monaco Editor.
 * It provides a WYSIWYG (What You See Is What You Get) editing experience,
 * with configurations customizable by DotCMS content type fields.
 */
@Component({
    selector: 'dot-wysiwyg-monaco',
    standalone: true,
    imports: [MonacoEditorModule, PaginatorModule, ReactiveFormsModule],
    templateUrl: './dot-wysiwyg-monaco.component.html',
    styleUrl: './dot-wysiwyg-monaco.component.scss',
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWysiwygMonacoComponent implements OnDestroy {
    /**
     * Holds a reference to the MonacoEditorComponent.
     */
    $editorRef = viewChild<MonacoEditorComponent>('editorRef');

    /**
     * Represents a required DotCMS content type field.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Represents the programming language to be used in the Monaco editor.
     * This variable sets the default language for code input and is initially set to `DEFAULT_MONACO_LANGUAGE`.
     * It can be customized by providing a different value through the alias 'language'.
     */
    $language = input<string>(DEFAULT_MONACO_LANGUAGE, { alias: 'language' });

    /**
     * A computed property that retrieves and parses custom Monaco properties that comes from
     * Field Variable with the name `monacoOptions`
     *
     */
    $customPropsContentField = computed(() => {
        const { fieldVariables } = this.$field();
        const { monacoOptions } = getFieldVariablesParsed<{ monacoOptions: string }>(
            fieldVariables
        );

        return stringToJson(monacoOptions);
    });

    /**
     * Represents an instance of the Monaco Code Editor.
     */
    #editor: monaco.editor.IStandaloneCodeEditor = null;

    /**
     * A computed property that generates the configuration options for the Monaco editor.
     *
     * This property merges default Monaco editor configurations with custom ones and sets the editor's language.
     *
     */
    $monacoOptions = computed(() => {
        return {
            ...DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG,
            ...this.$customPropsContentField(),
            language: this.$language()
        };
    });

    /**
     * A disposable reference that manages the lifecycle of content change listeners.
     * It starts as null and can be assigned a disposable object that will be used
     * to clean up event listeners or other resources related to content changes.
     *
     * @type {monaco.IDisposable | null}
     */
    #contentChangeDisposable: monaco.IDisposable | null = null;

    /**
     * Initializes the editor by setting up the editor reference,
     * processing the editor content, and setting up a listener for content changes.
     *
     * @return {void} No return value.
     */
    onEditorInit() {
        this.#editor = this.$editorRef().editor;
        this.processEditorContent();
        this.setupContentChangeListener();
    }

    private processEditorContent() {
        if (this.#editor) {
            const currentContent = this.#editor.getValue();

            const processedContent = this.removeWysiwygComment(currentContent);
            if (currentContent !== processedContent) {
                this.#editor.setValue(processedContent);
            }
        }
    }

    ngOnDestroy() {
        try {
            if (this.#contentChangeDisposable) {
                this.#contentChangeDisposable.dispose();
            }

            if (this.#editor) {
                this.removeEditor();
            }

            const model = this.#editor?.getModel();
            if (model && !model.isDisposed()) {
                model.dispose();
            }
        } catch (error) {
            console.error('Error during Monaco Editor cleanup:', error);
        }
    }

    private removeEditor() {
        this.#editor.dispose();
        this.#editor = null;
    }

    private removeWysiwygComment(content: string): string {
        const regex = new RegExp(`^\\s*${COMMENT_TINYMCE}\\s*`);

        return content.replace(regex, '');
    }

    private setupContentChangeListener() {
        if (this.#editor) {
            this.#contentChangeDisposable = this.#editor.onDidChangeModelContent(() => {
                this.processEditorContent();
            });
        }
    }
}
