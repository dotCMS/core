import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { of } from 'rxjs';
import { RawEditorOptions } from 'tinymce';

import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { catchError } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';

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
    imports: [
        EditorModule,
        FormsModule,
        ReactiveFormsModule,
        // TODO: `HttpClientModule` should not be imported into a component directly.
        // Please refactor the code to add `provideHttpClient()` call to the provider list in the
        // application bootstrap logic and remove the `HttpClientModule` import from this component.
        HttpClientModule
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        HttpClient,
        DialogService,
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

    private readonly http = inject(HttpClient);
    private readonly dotWysiwygPluginService = inject(DotWysiwygPluginService);

    private readonly configPath = '/api/vtl/tinymceprops';
    protected readonly init = signal<RawEditorOptions>(null);

    ngOnInit(): void {
        const { tinymceprops } = getFieldVariablesParsed(this.field.fieldVariables);
        const variables = stringToJson(tinymceprops as string);

        this.http
            .get<RawEditorOptions>(this.configPath)
            .pipe(catchError(() => of(null)))
            .subscribe((SYTEM_WIDE_CONFIG) => {
                const CONFIG = SYTEM_WIDE_CONFIG || DEFAULT_CONFIG;
                this.init.set({
                    setup: (editor) => this.dotWysiwygPluginService.initializePlugins(editor),
                    ...CONFIG,
                    ...variables,
                    theme: 'silver' // In the new version, there is only one theme, which is the default one. Docs: https://www.tiny.cloud/docs/tinymce/latest/editor-theme/
                });
            });
    }
}
