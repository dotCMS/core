import {
    MonacoEditorComponent,
    MonacoEditorConstructionOptions,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    signal,
    ViewChild
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { PaginatorModule } from 'primeng/paginator';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

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
export class DotWysiwygMonacoComponent {
    @ViewChild('editorRef', { static: true }) editorRef!: MonacoEditorComponent;

    /**
     * Represents a required DotCMS content type field.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    $codeLanguage = input<string>(DEFAULT_MONACO_LANGUAGE, { alias: 'language' });

    private _userMonacoOptions = signal<MonacoEditorConstructionOptions>({});

    #editor: monaco.editor.IStandaloneCodeEditor;

    $monacoOptions = computed(() => {
        return {
            ...DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG,
            ...this._userMonacoOptions(),
            language: this.$codeLanguage()
        };
    });

    onEditorInit() {
        this.#editor = this.editorRef.editor;
    }
}
