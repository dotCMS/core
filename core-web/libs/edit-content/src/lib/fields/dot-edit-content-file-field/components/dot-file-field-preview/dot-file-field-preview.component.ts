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

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { DialogModule } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';

import { catchError } from 'rxjs/operators';

import { DotMessageService, DotResourceLinksService } from '@dotcms/data-access';
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
import { getFileMetadata } from '@dotcms/utils';

import {
    DotPreviewResourceLink,
    UploadedFile
} from '../../../../models/dot-edit-content-file.model';
import { CONTENT_TYPES, DEFAULT_CONTENT_TYPE } from '../../dot-edit-content-file-field.const';

type FileInfo = UploadedFile & {
    contentType: string;
    /** Binary inline field variable when hydrated from contentlet metadata. */
    fieldVariable: string;
    downloadLink: string | null;
    content: string | null;
    metadata: DotFileMetadata;
};

const buildTempDownloadLink = (referenceUrl: string): string => {
    const separator = referenceUrl.includes('?') ? '&' : '?';

    return `${referenceUrl}${separator}force_download=true`;
};

@Component({
    selector: 'dot-file-field-preview',
    imports: [
        DotTempFileThumbnailComponent,
        DotFileSizeFormatPipe,
        DotMessagePipe,
        ButtonModule,
        ConfirmPopupModule,
        DialogModule,
        TooltipModule,
        DotCopyButtonComponent
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-file-field-preview.component.html',
    styleUrls: ['./dot-file-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotFileFieldPreviewComponent implements OnInit {
    readonly #dotResourceLinksService = inject(DotResourceLinksService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
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
     * Whether the "Edit image" action should be shown for the current file.
     * Driven by the parent based on field type, metadata and launcher availability.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $canEditImage = input<boolean>(false, { alias: 'canEditImage' });

    /**
     * Remove file
     *
     * @memberof DotFileFieldPreviewComponent
     */
    removeFile = output();

    /**
     * Edit image. Emitted when the user triggers the image editor action.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    editImage = output();

    /**
     * Edit file. Emitted when the user clicks the text file preview to open the editor.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    editFile = output();
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
            const inlineFieldVariable =
                typeof file['fieldVariable'] === 'string' ? file['fieldVariable'] : undefined;
            const contentType = CONTENT_TYPES[file.contentType] || DEFAULT_CONTENT_TYPE;
            const fieldVariable = inlineFieldVariable ?? contentType;

            return {
                source: previewFile.source,
                file,
                content: file.content,
                contentType,
                fieldVariable,
                downloadLink: `/contentAsset/raw-data/${file.inode}/${fieldVariable}?byInode=true&force_download=true`,
                metadata: getFileMetadata(file)
            };
        }

        const file = previewFile.file;

        return {
            source: previewFile.source,
            file,
            content: file.content ?? null,
            contentType: DEFAULT_CONTENT_TYPE,
            fieldVariable: DEFAULT_CONTENT_TYPE,
            downloadLink: file.referenceUrl ? buildTempDownloadLink(file.referenceUrl) : null,
            metadata: file.metadata
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
            this.fetchResourceLinks(fileInfo.file, fileInfo.fieldVariable);
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
     * Asks for confirmation before removing the file, displaying a ConfirmPopup
     * anchored to the triggering button. Emits {@link removeFile} only on accept.
     *
     * @param {Event} event The click event from the remove button.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    confirmRemove(event: Event): void {
        if (this.$disabled()) {
            return;
        }

        this.#confirmationService.confirm({
            target: event.currentTarget as EventTarget,
            message: this.#dotMessageService.get('dot.file.field.action.remove.confirm'),
            icon: 'pi pi-info-circle',
            closeOnEscape: true,
            acceptLabel: this.#dotMessageService.get('dot.common.remove'),
            rejectLabel: this.#dotMessageService.get('dot.common.cancel'),
            acceptButtonStyleClass: 'p-button-sm p-button-danger',
            rejectButtonStyleClass: 'p-button-sm p-button-text p-button-secondary',
            accept: () => this.removeFile.emit()
        });
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
