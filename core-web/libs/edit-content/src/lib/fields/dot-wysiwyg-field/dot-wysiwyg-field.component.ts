import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-wysiwyg-field',
    standalone: true,
    imports: [EditorModule, FormsModule, ReactiveFormsModule],
    templateUrl: './dot-wysiwyg-field.component.html',
    styleUrl: './dot-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [{ provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotWYSIWYGFieldComponent {
    @Input() field!: DotCMSContentTypeField;

    protected readonly plugins = signal(
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template'
    );
    protected readonly toolbar = signal(
        'paste print textpattern | insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image hr | preview | validation media | forecolor dotimageclipboard backcolor emoticons'
    );
}
