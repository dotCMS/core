import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { Editor, RawEditorOptions } from 'tinymce';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnDestroy
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { getFieldVariablesParsed, stringToJson } from '../../../../utils/functions.util';
import { DEFAULT_TINYMCE_CONFIG } from '../../dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

@Component({
    selector: 'dot-wysiwyg-tinymce',
    imports: [EditorComponent, ReactiveFormsModule],
    templateUrl: './dot-wysiwyg-tinymce.component.html',
    styleUrl: './dot-wysiwyg-tinymce.component.scss',
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        DialogService,
        DotWysiwygTinymceService,
        DotWysiwygPluginService,
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWysiwygTinymceComponent implements OnDestroy {
    #dotWysiwygPluginService = inject(DotWysiwygPluginService);
    #dotWysiwygTinymceService = inject(DotWysiwygTinymceService);

    /**
     * Represents a required DotCMS content type field.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * A computed property that retrieves and parses custom TinyMCE properties that comes from
     * Field Variable with the name `tinymceprops`
     *
     */
    $customPropsContentField = computed(() => {
        const { fieldVariables } = this.$field();
        const { tinymceprops } = getFieldVariablesParsed(fieldVariables);

        return stringToJson(tinymceprops as string);
    });

    /**
     * Represents a signal that contains the wide configuration properties for the TinyMCE WYSIWYG editor.
     */
    $wideConfig = toSignal<RawEditorOptions>(this.#dotWysiwygTinymceService.getProps());

    /**
     * A computed property that generates the configuration object for the TinyMCE editor.
     * This configuration merges default settings, wide configuration settings,
     * and custom properties specific to the content field. Additionally, it sets
     * up the editor with initial plugins using the `dotWysiwygPluginService`.
     */
    $editorConfig = computed<RawEditorOptions>(() => {
        const config: RawEditorOptions = {
            ...DEFAULT_TINYMCE_CONFIG,
            ...(this.$wideConfig() || {}),
            ...this.$customPropsContentField(),
            setup: (editor) => {
                this.#dotWysiwygPluginService.initializePlugins(editor);
            }
        };

        return config;
    });

    /**
     * Inserts content into the TinyMCE editor.
     *
     * @param {string} content - The content to insert into the editor.
     */
    insertContent(content: string): void {
        if (this.#editor) {
            this.#editor.execCommand('mceInsertContent', false, content);
        }
    }

    /**
     * The #editor variable represents an instance of the Editor class, which provides functionality for text editing.
     */
    #editor: Editor = null;

    /**
     * Handles the initialization of the editor instance.
     */
    handleEditorInit(event: { editor: Editor }): void {
        this.#editor = event.editor;
    }

    ngOnDestroy(): void {
        if (this.#editor) {
            this.removeEditor();
        }
    }

    private removeEditor(): void {
        this.#editor.remove();
        this.#editor = null;
    }
}
