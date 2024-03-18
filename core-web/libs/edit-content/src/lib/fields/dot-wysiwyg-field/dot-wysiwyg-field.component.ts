import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { Editor, TinyMCE } from 'tinymce';

import { ChangeDetectionStrategy, Component, Input, inject, signal, NgZone } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotAssetSearchDialogComponent } from './dot-asset-search-dialog/dot-asset-search-dialog.component';

declare global {
    interface Window {
        tinymce: TinyMCE;
    }
}

@Component({
    selector: 'dot-wysiwyg-field',
    standalone: true,
    imports: [EditorModule, FormsModule, ReactiveFormsModule],
    templateUrl: './dot-wysiwyg-field.component.html',
    styleUrl: './dot-wysiwyg-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService, { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotWYSIWYGFieldComponent {
    @Input() field!: DotCMSContentTypeField;
    public init = {
        setup: (editor) => {
            this.setup(editor);
        }
    };
    private readonly dialogService: DialogService = inject(DialogService);
    private readonly ngZone: NgZone = inject(NgZone);

    protected readonly plugins = signal(
        'dotAddImage advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template'
    );

    protected readonly toolbar = signal(
        'buttondotAddImage paste print textpattern | insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image hr | preview | validation media | forecolor backcolor emoticons'
    );

    setup(_editor: Editor) {
        window.tinymce.PluginManager.add('dotAddImage', (editor: Editor) => {
            editor.ui.registry.addButton('buttondotAddImage', {
                text: 'Add Image',
                onAction: () => {
                    this.ngZone.run(() => {
                        const ref = this.dialogService.open(DotAssetSearchDialogComponent, {
                            header: 'Add Image',
                            width: '800px',
                            height: '500px',
                            contentStyle: { padding: 0 }
                        });

                        ref.onClose
                            .pipe(
                                take(1),
                                filter((asset) => !!asset)
                            )
                            .subscribe((asset) =>
                                editor.insertContent(
                                    `<img src="${asset.assetVersion || asset.asset}" alt="${
                                        asset.title
                                    }" />`
                                )
                            );
                    });
                }
            });
        });
    }
}
