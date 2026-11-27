import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { Editor, RawEditorOptions } from 'tinymce';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnDestroy,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { defaultIfEmpty } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { getFieldVariablesParsed, stringToJson } from '../../../../utils/functions.util';
import { DEFAULT_TINYMCE_CONFIG } from '../../dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

@Component({
    selector: 'dot-wysiwyg-tinymce',
    imports: [DotMessagePipe, EditorComponent, ProgressSpinnerModule, ReactiveFormsModule],
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
     * Whether the field has an error.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });

    /**
     * Whether the TinyMCE editor iframe currently has focus.
     * Tracked via TinyMCE's focus/blur events because `:focus-within`
     * does not propagate from iframes in Chrome.
     */
    $isFocused = signal(false);

    /**
     * Whether TinyMCE has finished initialising (`onInit`). Until then — while the
     * system-wide config loads and while TinyMCE builds its iframe — the field shows a
     * loading overlay in the editor's place, so the form neither jumps nor looks broken.
     */
    $isEditorReady = signal(false);

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
     * System-wide TinyMCE configuration served by `/api/vtl/tinymceprops` (`get.vtl`).
     * `undefined` while the request is in flight; `null` when it failed (no `get.vtl`,
     * no scripting permission, server error), which the service maps for us, and also when
     * the request completes without a value (cancelled), so the editor never waits forever.
     */
    $wideConfig = toSignal<RawEditorOptions | null>(
        this.#dotWysiwygTinymceService.getProps().pipe(defaultIfEmpty(null))
    );

    /**
     * Whether the system-wide configuration request has settled (answered or failed).
     *
     * `<editor>` reads `init` once, when it calls `tinymce.init()`, and never again, so it
     * must not exist before the system-wide config is known: creating it earlier silently
     * drops `get.vtl` whenever TinyMCE is already loaded (every in-app navigation and most
     * full reloads). A failed request still settles, so the editor always opens.
     */
    $configReady = computed(() => this.$wideConfig() !== undefined);

    /**
     * A computed property that generates the configuration object for the TinyMCE editor.
     * This configuration merges default settings, wide configuration settings,
     * and custom properties specific to the content field. Additionally, it sets
     * up the editor with initial plugins using the `dotWysiwygPluginService`.
     */
    $editorConfig = computed<RawEditorOptions>(() => {
        const config: RawEditorOptions = {
            ...DEFAULT_TINYMCE_CONFIG,
            // Browse buttons in the image and link dialogs. Before the configs below so they can
            // still narrow `file_picker_types`; the callback itself is a function, which no JSON
            // config can express or override.
            ...this.#dotWysiwygPluginService.filePickerOptions(),
            ...(this.$wideConfig() || {}),
            ...this.$customPropsContentField(),
            // TinyMCE renders into an iframe, which no label outside can name. Two surfaces need
            // the field's name: `iframe_aria_text` labels the body inside the iframe, which is what
            // a screen reader announces once inside the editing area, and `iframe_attrs.title`
            // names the frame itself, which it announces on the way in. TinyMCE hardcodes that
            // title to "Rich Text Area" otherwise — identical for every rich-text field on the
            // form. Set after the spreads so a system-wide or per-field config cannot leave the
            // editor unnamed.
            iframe_aria_text: this.$field()?.name,
            iframe_attrs: { title: this.$field()?.name },
            // TinyMCE 6 shows an "Upgrade" promotion in the toolbar by default. Not in
            // dotCMS: forced off after the spreads so no system-wide or field config can
            // turn it back on.
            promotion: false,
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
        this.$isEditorReady.set(true);
        this.#editor.on('focus', () => this.$isFocused.set(true));
        this.#editor.on('blur', () => this.$isFocused.set(false));
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
