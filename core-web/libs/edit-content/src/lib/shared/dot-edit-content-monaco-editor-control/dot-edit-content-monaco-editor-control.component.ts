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
import { ControlContainer, FormControl, ReactiveFormsModule, Validator } from '@angular/forms';

import { PaginatorModule } from 'primeng/paginator';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { ensureDotVelocityLanguageRegistered } from '@dotcms/ui';

import {
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity
} from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.utils';
import {
    AvailableLanguageMonaco,
    DEFAULT_MONACO_CONFIG,
    DEFAULT_MONACO_LANGUAGE
} from '../../models/dot-edit-content-field.constant';
import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';

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
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentMonacoEditorControlComponent implements OnDestroy {
    #monacoLoaderService: MonacoEditorLoaderService = inject(MonacoEditorLoaderService);
    #ngZone: NgZone = inject(NgZone);
    #controlContainer: ControlContainer = inject(ControlContainer);

    /**
     * Holds a reference to the MonacoEditorComponent.
     */
    $monacoEditorComponentRef = viewChild<MonacoEditorComponent>('editorRef');

    /**
     * Represents a required DotCMS content type field.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * A signal that holds the error state of the Monaco editor.
     * It is used to display the error state of the Monaco editor.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * Input property to force a specific language for the Monaco editor.
     * If provided, this overrides the automatic language detection.
     */
    $forcedLanguage = input<AvailableLanguageMonaco | null>(null, { alias: 'forceLanguage' });
    /**
     * Whether the editor's syntax markers (the red squiggles) make the field invalid and so block
     * Save. Off by default: the language is often guessed from the content, and a wrong guess
     * covers valid content in the wrong language's errors (issue #37996). Fields whose language
     * is fixed and whose syntax must be valid, such as JSON, turn it on.
     */
    $syntaxErrorsBlockSave = input<boolean>(false, { alias: 'syntaxErrorsBlockSave' });

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
     * The language is chosen in this order: the `forceLanguage` input, then `language` in the
     * field's `monacoOptions` variable, then the language detected from the content.
     */
    $monacoOptions = computed(() => {
        const customProps = this.$customPropsContentField();

        return {
            ...DEFAULT_MONACO_CONFIG,
            ...customProps,
            // Monaco owns its DOM, so `<label for>` cannot reach the surface it renders. Its own
            // ariaLabel option is where the field name belongs; without it the editor announces
            // itself with Monaco's generic default and the author cannot tell which field they are
            // in. Set after the spreads, like `language`, so it is not silently dropped by custom
            // monacoOptions.
            ariaLabel: this.$field()?.name,
            // A language configured in monacoOptions pins the editor, so content whose words look
            // like another language (issue #37996) is not re-detected and checked as that language.
            language: this.$forcedLanguage() || customProps?.language || this.$language()
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

        this.applySyntaxErrorPolicy();
        this.detectLanguage();
        this.setupContentChangeListener();
    }

    /**
     * Stops the editor's syntax markers from invalidating the field, unless
     * `syntaxErrorsBlockSave` is set.
     *
     * `@materia-ui/ngx-monaco-editor` registers itself as a form validator and returns a `monaco`
     * error whenever the editor shows any marker, of any severity. Angular calls that validator
     * through the instance (`c => validator.validate(c)`), so replacing `validate` here takes
     * effect for every later check. The replacement reads `syntaxErrorsBlockSave` on each check,
     * so a later change to the input applies from the next validation. The library runs `init`
     * once per component instance, so the original is never wrapped twice.
     */
    private applySyntaxErrorPolicy(): void {
        const editorComponent = this.$monacoEditorComponentRef();
        if (!editorComponent) {
            return;
        }

        const libraryValidate = editorComponent.validate.bind(editorComponent);
        // Typed as Angular's `Validator`, whose `validate` may return null ("valid"); the
        // library's own declaration omits the null.
        const editorValidator: Validator = editorComponent;
        editorValidator.validate = () => (this.$syntaxErrorsBlockSave() ? libraryValidate() : null);

        // Clears a `monaco` error recorded before the replacement above. Real Monaco computes
        // markers after `init`, so this matters mainly for the test mock, which reports its
        // marker synchronously while the editor is being created.
        const control = this.formControl;
        if (control?.hasError('monaco')) {
            control.updateValueAndValidity();
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
     * Registers the Velocity language for the Monaco editor: the shared grammar from `@dotcms/ui`,
     * under the `velocity` id this field's detection and forced language use.
     */
    registerVelocityLanguage() {
        this.#ngZone.runOutsideAngular(() => ensureDotVelocityLanguageRegistered());
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

    get formControl() {
        const control = this.#controlContainer.control?.get(this.$field().variable) as FormControl;
        return control;
    }
}
