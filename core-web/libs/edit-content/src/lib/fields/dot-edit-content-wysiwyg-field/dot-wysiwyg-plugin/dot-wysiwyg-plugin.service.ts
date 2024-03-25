import { Editor } from 'tinymce';

import { Injectable, NgZone, inject } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { filter } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchDialogComponent } from '@dotcms/ui';

import { formatDotImageNode } from './utils/editor.utils';

@Injectable()
export class DotWysiwygPluginService {
    private readonly dialogService: DialogService = inject(DialogService);
    private readonly dotUploadFileService: DotUploadFileService = inject(DotUploadFileService);
    private readonly ngZone: NgZone = inject(NgZone);

    initializePlugins(editor: Editor): void {
        this.dotImagePlugin(editor);
    }

    private dotImagePlugin(editor: Editor): void {
        editor.ui.registry.addButton('dotAddImage', {
            icon: 'image',
            onAction: () => {
                this.ngZone.run(() => {
                    const ref = this.dialogService.open(DotAssetSearchDialogComponent, {
                        header: 'Insert Image',
                        width: '800px',
                        height: '500px',
                        contentStyle: { padding: 0 },
                        data: {
                            assetType: 'image'
                        }
                    });

                    ref.onClose
                        .pipe(filter((asset) => !!asset))
                        .subscribe((asset: DotCMSContentlet) =>
                            editor.insertContent(formatDotImageNode(asset))
                        );
                });
            }
        });

        this.dotFilePlugin(editor);
    }

    private dotFilePlugin(editor: Editor) {
        editor.on('drop', async (event) => {
            const file = event.dataTransfer.files[0];

            // Check if the file is an image
            if (!file.type.includes('image')) {
                return;
            }

            event.preventDefault();
            event.stopImmediatePropagation();
            event.stopPropagation();

            this.dotUploadFileService
                .publishContent({
                    data: file
                })
                .subscribe((contentlets) => {
                    const data = contentlets[0];
                    const asset = data[Object.keys(data)[0]];
                    editor.insertContent(formatDotImageNode(asset));
                });
        });
    }
}
