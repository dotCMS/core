import { of } from 'rxjs';

import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    output,
    signal,
    OnInit
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { catchError } from 'rxjs/operators';

import { DotResourceLinksService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotTempFileThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotCopyButtonComponent
} from '@dotcms/ui';

import { DotPreviewResourceLink, PreviewFile } from '../../models';
import { getFileMetadata } from '../../utils';

@Component({
    selector: 'dot-file-field-preview',
    standalone: true,
    imports: [
        DotTempFileThumbnailComponent,
        DotFileSizeFormatPipe,
        DotMessagePipe,
        ButtonModule,
        DialogModule,
        DotCopyButtonComponent
    ],
    providers: [],
    templateUrl: './dot-file-field-preview.component.html',
    styleUrls: ['./dot-file-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotFileFieldPreviewComponent implements OnInit {
    readonly #dotResourceLinksService = inject(DotResourceLinksService);
    /**
     * Preview file
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $previewFile = input.required<PreviewFile>({ alias: 'previewFile' });
    /**
     * Remove file
     *
     * @memberof DotFileFieldPreviewComponent
     */
    removeFile = output();
    /**
     * Show dialog
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $showDialog = signal(false);
    /**
     * File metadata
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $metadata = computed(() => {
        const previewFile = this.$previewFile();
        if (previewFile.source === 'temp') {
            return previewFile.file.metadata;
        }

        return getFileMetadata(previewFile.file);
    });
    /**
     * Content
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $content = computed(() => {
        const previewFile = this.$previewFile();
        if (previewFile.source === 'contentlet') {
            return previewFile.file.content;
        }

        return null;
    });
    /**
     * Download link
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $downloadLink = computed(() => {
        const previewFile = this.$previewFile();
        if (previewFile.source === 'contentlet') {
            const file = previewFile.file;

            return `/contentAsset/raw-data/${file.inode}/asset?byInode=true&force_download=true`;
        }

        return null;
    });

    /**
     * Resource links
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $resourceLinks = signal<DotPreviewResourceLink[]>([]);

    /**
     * OnInit lifecycle hook.
     *
     * If the source is 'contentlet', calls {@link fetchResourceLinks} to fetch the resource links for the file.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    ngOnInit() {
        const previewFile = this.$previewFile();

        if (previewFile.source === 'contentlet') {
            this.fetchResourceLinks(previewFile.file);
        }
    }

    /**
     * Toggle the visibility of the dialog.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    toggleShowDialog() {
        this.$showDialog.update((value) => !value);
    }

    /**
     * Downloads the file at the given link.
     *
     * @param {string} link The link to the file to download.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    downloadAsset(link: string): void {
        window.open(link, '_self');
    }

    /**
     * Fetches the resource links for the given contentlet.
     *
     * @private
     * @param {DotCMSContentlet} contentlet The contentlet to fetch the resource links for.
     * @memberof DotFileFieldPreviewComponent
     */
    private fetchResourceLinks(contentlet: DotCMSContentlet): void {
        this.#dotResourceLinksService
            .getFileResourceLinks({
                fieldVariable: 'asset',
                inodeOrIdentifier: contentlet.identifier
            })
            .pipe(
                catchError(() => {
                    return of({
                        configuredImageURL: '',
                        text: '',
                        versionPath: '',
                        idPath: ''
                    });
                })
            )
            .subscribe(({ configuredImageURL, text, versionPath, idPath }) => {
                const fileLink = configuredImageURL
                    ? `${window.location.origin}${configuredImageURL}`
                    : '';

                const options = [
                    {
                        key: 'FileLink',
                        value: fileLink
                    },
                    {
                        key: 'VersionPath',
                        value: versionPath
                    },
                    {
                        key: 'IdPath',
                        value: idPath
                    }
                ];

                if (contentlet.baseType === DotCMSBaseTypesContentTypes.FILEASSET) {
                    options.push({
                        key: 'Resource-Link',
                        value: text
                    });
                }

                this.$resourceLinks.set(options);
            });
    }
}
