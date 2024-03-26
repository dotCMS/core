import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { RawEditorOptions } from 'tinymce';

import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

import { getFieldVariablesParsed } from '../../utils/functions.util';

/**
 * WYSIWYG editor themes
 */
enum WysiwygEditorTheme {
    silver = 'silver'
}

@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    standalone: true,
    imports: [EditorModule, FormsModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
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

    private readonly dotWysiwygPluginService = inject(DotWysiwygPluginService);
    protected readonly plugins = signal(
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template'
    );

    protected readonly toolbar = signal(
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent image hr'
    );

    protected init: RawEditorOptions;

    ngOnInit(): void {
        const variables = getFieldVariablesParsed(this.field.fieldVariables);
        const theme = this.getValidTheme(variables?.theme as string);

        this.init = {
            menubar: false,
            image_caption: true,
            image_advtab: true,
            contextmenu: 'align link image',
            toolbar1: this.toolbar(),
            plugins: this.plugins(),
            ...variables,
            theme,
            setup: (editor) => this.dotWysiwygPluginService.initializePlugins(editor)
        };
    }

    private getValidTheme(theme: string): string {
        const poisbleThemes = Object.values(WysiwygEditorTheme) as string[];

        return poisbleThemes.includes(theme) ? theme : '';
    }
}
