import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { Editor, RawEditorOptions } from 'tinymce';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnDestroy,
    OnInit,
    signal
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { getFieldVariablesParsed, stringToJson } from '../../../../utils/functions.util';
import { DEFAULT_TINYMCE_CONFIG } from '../../dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

@Component({
    selector: 'dot-wysiwyg-tinymce',
    standalone: true,
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
export class DotWysiwygTinymceComponent implements OnInit, OnDestroy {
    #dotWysiwygPluginService = inject(DotWysiwygPluginService);
    #dotWysiwygTinymceService = inject(DotWysiwygTinymceService);

    /**
     * A reactive signal holding the initial configuration options for the editor.
     *
     * Type: signal<RawEditorOptions>
     */
    $editorOptions = signal<RawEditorOptions>({});

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
     * The #editor variable represents an instance of the Editor class, which provides functionality for text editing.
     */
    #editor: Editor;

    ngOnInit(): void {
        this.initializeEditor();
    }

    handleEditorInit(event: { editor: Editor }): void {
        this.#editor = event.editor;
    }

    ngOnDestroy(): void {
        if (this.#editor) {
            this.removeEditor();
        }
    }

    private initializeEditor(): void {
        this.#dotWysiwygTinymceService.getProps().subscribe((SYSTEM_WIDE_CONFIG) => {
            this.$editorOptions.set({
                ...DEFAULT_TINYMCE_CONFIG,
                ...(SYSTEM_WIDE_CONFIG || {}),
                ...this.$customPropsContentField(),
                setup: (editor) => this.#dotWysiwygPluginService.initializePlugins(editor)
            });
        });
    }

    private removeEditor(): void {
        this.#editor.remove();
        this.#editor = null;
    }
}
