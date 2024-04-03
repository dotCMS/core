import { Editor } from 'tinymce';

import { DestroyRef, Injectable, NgZone, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DialogService } from 'primeng/dynamicdialog';

import { filter } from 'rxjs/operators';

import { DotPropertiesService, DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchDialogComponent } from '@dotcms/ui';

import { DEFAULT_IMAGE_URL_PATTERN, formatDotImageNode } from './utils/editor.utils';

/**
 * Service to initialize the plugins for the WYSIWYG editor
 *
 * @export
 * @class DotWysiwygPluginService
 */
@Injectable()
export class DotWysiwygPluginService {
    private readonly dialogService: DialogService = inject(DialogService);
    private readonly dotUploadFileService: DotUploadFileService = inject(DotUploadFileService);
    private readonly dotPropertiesService: DotPropertiesService = inject(DotPropertiesService);
    private readonly ngZone: NgZone = inject(NgZone);

    private IMAGE_URL_PATTERN = DEFAULT_IMAGE_URL_PATTERN;

    private readonly destroyRef$ = inject(DestroyRef);

    constructor() {
        this.dotPropertiesService
            .getKey('WYSIWYG_IMAGE_URL_PATTERN')
            .pipe(
                takeUntilDestroyed(this.destroyRef$),
                filter((IMAGE_URL_PATTERN) => !!IMAGE_URL_PATTERN)
            )
            .subscribe((IMAGE_URL_PATTERN) => (this.IMAGE_URL_PATTERN = IMAGE_URL_PATTERN));
    }

    /**
     * Initialize the plugins for the WYSIWYG editor
     * It should be called in the setup of the editor
     *
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    initializePlugins(editor: Editor): void {
        this.dotImagePlugin(editor);
    }

    /**
     * Add the image plugin to the editor
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private dotImagePlugin(editor: Editor): void {
        editor.ui.registry.addButton('dotAddImage', {
            icon: 'image',
            onAction: () => this.dotImageDialog(editor)
        });
        this.handleImageDrop(editor);
    }

    /**
     * Open the image dialog
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private dotImageDialog(editor: Editor): void {
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
                    editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset))
                );
        });
    }

    /**
     *  Handle the drop event in the editor
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private handleImageDrop(editor: Editor) {
        editor.on('drop', (event) => {
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
                    editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset));
                });
        });
    }
}
