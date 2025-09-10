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
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotFileMetadata
} from '@dotcms/dotcms-models';
import {
    DotTempFileThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotCopyButtonComponent
} from '@dotcms/ui';

import {
    DotPreviewResourceLink,
    UploadedFile
} from '../../../../models/dot-edit-content-file.model';
import { CONTENT_TYPES, DEFAULT_CONTENT_TYPE } from '../../dot-edit-content-file-field.const';
import { getFileMetadata } from '../../utils';

type FileInfo = UploadedFile & {
    contentType: string;
    downloadLink: string;
    content: string | null;
    metadata: DotFileMetadata;
};

@Component({
    selector: 'dot-file-field-preview',
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
    $previewFile = input.required<UploadedFile>({ alias: 'previewFile' });

    /**
     * Whether the component is disabled
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $disabled = input<boolean>(false, { alias: 'disabled' });

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
     * File info
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $fileInfo = computed<FileInfo>(() => {
        const previewFile = this.$previewFile();

        if (previewFile.source === 'contentlet') {
            const file = previewFile.file;

            const contentType = CONTENT_TYPES[file.contentType] || DEFAULT_CONTENT_TYPE;

            return {
                source: previewFile.source,
                file,
                content: file.content,
                contentType,
                downloadLink: `/contentAsset/raw-data/${file.inode}/${contentType}?byInode=true&force_download=true`,
                metadata: getFileMetadata(file)
            };
        }

        return {
            source: previewFile.source,
            file: previewFile.file,
            content: null,
            contentType: DEFAULT_CONTENT_TYPE,
            downloadLink: null,
            metadata: previewFile.file.metadata
        };
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
        const fileInfo = this.$fileInfo();

        if (fileInfo.source === 'contentlet') {
            this.fetchResourceLinks(fileInfo.file, fileInfo.contentType);
        }
    }

    /**
     * Toggle the visibility of the dialog.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    toggleShowDialog() {
        if (this.$disabled()) {
            return;
        }

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
        if (this.$disabled()) {
            return;
        }

        window.open(link, '_self');
    }

    /**
     * Fetches the resource links for the given contentlet.
     *
     * @private
     * @param {DotCMSContentlet} contentlet The contentlet to fetch the resource links for.
     * @memberof DotFileFieldPreviewComponent
     */
    private fetchResourceLinks(contentlet: DotCMSContentlet, contentType: string): void {
        this.#dotResourceLinksService
            .getFileResourceLinksByInode({
                fieldVariable: contentType,
                inode: contentlet.inode
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
