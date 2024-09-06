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
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

const DEFAULT_CONFIG = {
    menubar: false,
    image_caption: true,
    image_advtab: true,
    contextmenu: 'align link image',
    toolbar1:
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr',
    plugins:
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template',
    theme: 'silver'
};

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
    $init = signal<RawEditorOptions>({});

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
        const { tinymceprops } = getFieldVariablesParsed(this.$field().fieldVariables);

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
            this.$init.set({
                ...DEFAULT_CONFIG,
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
