import { Editor } from 'tinymce';

import { Injectable, NgZone, inject } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchDialogComponent } from '@dotcms/ui';

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
            text: 'Dot Image',
            icon: 'image',
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
                        .subscribe((asset: DotCMSContentlet) =>
                            editor.insertContent(
                                `<img src="${asset.assetVersion || asset.asset}" alt="${
                                    asset.title
                                }" />`
                            )
                        );
                });
            }
        });

        this.dotFilePlugin(editor);
    }

    private dotFilePlugin(editor: Editor) {
        editor.on('drop', async (event) => {
            // get image
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
                .subscribe((contentlets: DotCMSContentlet[]) => {
                    const data = contentlets[0];
                    const contentlet = data[Object.keys(data)[0]];

                    editor.insertContent(
                        `<img src="${contentlet.assetVersion || contentlet.asset}" alt="${
                            contentlet.title
                        }" />`
                    );
                });
        });
    }
}
