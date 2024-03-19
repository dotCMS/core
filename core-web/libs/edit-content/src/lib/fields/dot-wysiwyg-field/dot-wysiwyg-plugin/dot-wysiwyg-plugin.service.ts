import { Editor } from 'tinymce';

import { Injectable, NgZone, inject } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchDialogComponent } from '@dotcms/ui';

@Injectable()
export class DotWysiwygPluginService {
    private readonly dialogService: DialogService = inject(DialogService);
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
    }
}
