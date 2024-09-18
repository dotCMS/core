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
    DEFAULT_MONACO_LANGUAGE,
    DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG
} from '../../dot-edit-content-wysiwyg-field.constant';

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

    onEditorInit() {
        this.#editor = this.$editorRef().editor;
    }

    ngOnDestroy() {
        try {
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
}
