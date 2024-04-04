import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { of } from 'rxjs';
import { RawEditorOptions } from 'tinymce';

import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { catchError } from 'rxjs/operators';

import { DotScriptingApiService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

import { getFieldVariablesParsed } from '../../utils/functions.util';

const DEFAULT_CONFIG = {
    menubar: false,
    image_caption: true,
    image_advtab: true,
    contextmenu: 'align link image',
    toolbar1:
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr',
    plugins:
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template'
};

@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    standalone: true,
    imports: [EditorModule, FormsModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        DialogService,
        DotScriptingApiService,
        DotWysiwygPluginService,
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }
    ],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentWYSIWYGFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    private readonly dotWysiwygPluginService = inject(DotWysiwygPluginService);
    private readonly dotScriptingApiService = inject(DotScriptingApiService);

    protected init = signal<RawEditorOptions>(null);

    ngOnInit(): void {
        const variables = getFieldVariablesParsed(this.field.fieldVariables);
        this.dotScriptingApiService
            .get<RawEditorOptions>('tinymceprops')
            .pipe(catchError(() => of({})))
            .subscribe((GLOBAL_CONFIG = {}) => {
                this.init.set({
                    setup: (editor) => this.dotWysiwygPluginService.initializePlugins(editor),
                    ...DEFAULT_CONFIG,
                    ...GLOBAL_CONFIG,
                    ...variables,
                    theme: 'silver' // In the new version, there is only one theme, which is the default one. Docs: https://www.tiny.cloud/docs/tinymce/latest/editor-theme/
                });
            });
    }
}
