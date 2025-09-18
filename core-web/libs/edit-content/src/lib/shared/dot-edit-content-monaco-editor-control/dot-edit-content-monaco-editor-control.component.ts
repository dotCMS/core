import {
    MonacoEditorComponent,
    MonacoEditorLoaderService,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';
import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    NgZone,
    OnDestroy,
    signal,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';

import { PaginatorModule } from 'primeng/paginator';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { dotVelocityLanguageDefinition } from '../../custom-languages/velocity-monaco-language';
import {
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity
} from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.utils';
import { BaseFieldComponent } from '../../fields/shared/base-field.component';
import {
    AvailableLanguageMonaco,
    DEFAULT_MONACO_CONFIG,
    DEFAULT_MONACO_LANGUAGE
} from '../../models/dot-edit-content-field.constant';
import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';

interface WindowWithMonaco extends Window {
    monaco?: {
        languages: {
            register: (language: { id: string }) => void;
            setMonarchTokensProvider: (id: string, provider: unknown) => void;
        };
    };
}

/**
 * DotEditContentMonacoEditorControl is an Angular component utilizing Monaco Editor.
 * It provides a code editing experience with syntax highlighting and advanced features,
 * with configurations customizable by DotCMS content type fields.
 */
@Component({
    selector: 'dot-edit-content-monaco-editor-control',
    imports: [MonacoEditorModule, PaginatorModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-monaco-editor-control.component.html',
    styleUrl: './dot-edit-content-monaco-editor-control.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentMonacoEditorControlComponent
    extends BaseFieldComponent
    implements OnDestroy
{
    #monacoLoaderService: MonacoEditorLoaderService = inject(MonacoEditorLoaderService);
    #ngZone: NgZone = inject(NgZone);

    /**
     * Holds a reference to the MonacoEditorComponent.
     */
    $monacoEditorComponentRef = viewChild<MonacoEditorComponent>('editorRef');

    /**
     * Represents a required DotCMS content type field.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Input property to force a specific language for the Monaco editor.
     * If provided, this overrides the automatic language detection.
     */
    $forcedLanguage = input<AvailableLanguageMonaco | null>(null, { alias: 'forceLanguage' });

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
            ...DEFAULT_MONACO_CONFIG,
            ...this.$customPropsContentField(),
            language: this.$forcedLanguage() || this.$language()
        };
    });

    /**
     * A signal that holds the current language of the Monaco editor.
     * It starts with the default language, which is 'plaintext'.
     */
    $language = signal<string>(DEFAULT_MONACO_LANGUAGE);

    /**
     * A disposable reference that manages the lifecycle of content change listeners.
     * It starts as null and can be assigned a disposable object that will be used
     * to clean up event listeners or other resources related to content changes.
     *
     * @type {monaco.IDisposable | null}
     */
    #contentChangeDisposable: monaco.IDisposable | null = null;

    /**
     * A signal that holds the loading state of the Monaco editor.
     * It starts as null and can be assigned a boolean value that will be used
     * to determine if the Monaco editor is loaded.
     */
    $isMonacoLoaded = toSignal(this.#monacoLoaderService.isMonacoLoaded$, {
        requireSync: true
    });

    constructor() {
        super();
        this.handleMonacoLoaded(this.$isMonacoLoaded);
    }

    /**
     * Inserts content into the Monaco editor.
     *
     * @param {string} content - The content to insert into the editor.
     */
    insertContent(content: string): void {
        if (this.#editor) {
            const selection = this.#editor.getSelection();
            const range = new monaco.Range(
                selection.startLineNumber,
                selection.startColumn,
                selection.endLineNumber,
                selection.endColumn
            );
            const id = { major: 1, minor: 1 };
            const op = { identifier: id, range, text: content, forceMoveMarkers: true };
            this.#editor.executeEdits('my-source', [op]);
        }
    }

    /**
     * Initializes the editor by setting up the editor reference,
     * processing the editor content, and setting up a listener for content changes.
     *
     * @return {void} No return value.
     */
    onEditorInit($editorRef: monaco.editor.IStandaloneCodeEditor) {
        this.#editor = $editorRef;

        this.detectLanguage();
        this.setupContentChangeListener();
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

    /**
     * Sets up a listener for content changes in the Monaco editor.
     */
    private setupContentChangeListener() {
        if (this.#editor) {
            this.#contentChangeDisposable = this.#editor.onDidChangeModelContent(() => {
                this.detectLanguage();
            });
        }
    }

    /**
     * Sets the language of the Monaco editor.
     *
     * @param {string} language - The language to set for the editor.
     */
    private setLanguage(language: string) {
        this.$language.set(language);
    }

    /**
     * Registers the Velocity language for the Monaco editor.
     */
    registerVelocityLanguage() {
        this.#ngZone.runOutsideAngular(() => {
            const windowWithMonaco = window as WindowWithMonaco;
            if (windowWithMonaco.monaco) {
                windowWithMonaco.monaco.languages.register({
                    id: AvailableLanguageMonaco.Velocity
                });
                windowWithMonaco.monaco.languages.setMonarchTokensProvider(
                    AvailableLanguageMonaco.Velocity,
                    dotVelocityLanguageDefinition
                );
            } else {
                console.warn('Monaco is not available globally');
            }
        });
    }

    private readonly languageDetectors = {
        [AvailableLanguageMonaco.Velocity]: isVelocity,
        [AvailableLanguageMonaco.Javascript]: isJavascript,
        [AvailableLanguageMonaco.Html]: isHtml,
        [AvailableLanguageMonaco.Markdown]: isMarkdown
    };

    /**
     * Detects the language of the content in the Monaco editor and sets the appropriate language.
     */
    private detectLanguage() {
        // Skip language detection if a forced language is provided
        if (this.$forcedLanguage()) {
            this.setLanguage(this.$forcedLanguage());

            return;
        }

        const content = this.#editor.getValue().trim();

        if (!content) {
            this.setLanguage(AvailableLanguageMonaco.PlainText);

            return;
        }

        const detectedLanguage = Object.entries(this.languageDetectors).find(([, detector]) =>
            detector(content)
        )?.[0] as AvailableLanguageMonaco;

        this.setLanguage(detectedLanguage || AvailableLanguageMonaco.PlainText);
    }

    writeValue(_: unknown): void {
        // noop
    }

    /**
     * Handles the loading state of the Monaco editor.
     *
     * @param {boolean} isLoaded - The loading state of the Monaco editor.
     */
    readonly handleMonacoLoaded = signalMethod<boolean>((isLoaded) => {
        if (!isLoaded) {
            return;
        }

        this.registerVelocityLanguage();
    });
}
