import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError } from 'rxjs/operators';

import { DotResourceLinksService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSTempFile,
    DotFileMetadata
} from '@dotcms/dotcms-models';
import {
    DotCopyButtonComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotTempFileThumbnailComponent
} from '@dotcms/ui';

import { getFileMetadata } from '../../utils/binary-field-utils';

export enum EDITABLE_FILE {
    image = 'image',
    text = 'text',
    unknown = 'unknown'
}

interface dotPreviewResourceLink {
    key: string;
    value: string;
    show: boolean;
}

@Component({
    selector: 'dot-binary-field-preview',
    imports: [
        CommonModule,
        ButtonModule,
        SkeletonModule,
        DotTempFileThumbnailComponent,
        DialogModule,
        DotMessagePipe,
        DotFileSizeFormatPipe,
        DotCopyButtonComponent
    ],
    providers: [DotResourceLinksService],
    templateUrl: './dot-binary-field-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotBinaryFieldPreviewComponent {
    $contentlet = input<DotCMSContentlet>(undefined, { alias: 'contentlet' });
    $tempFile = input<DotCMSTempFile>(undefined, { alias: 'tempFile' });
    $editableImage = input<boolean>(undefined, { alias: 'editableImage' });
    $fieldVariable = input<string>(undefined, { alias: 'fieldVariable' });
    $disabled = input<boolean>(false, { alias: 'disabled' });

    $editImage = output<void>();
    $editFile = output<void>();
    $removeFile = output<void>();

    protected visibility = false;
    protected isEditable = false;
    protected readonly content = signal<string>('');
    protected readonly resourceLinks = signal<dotPreviewResourceLink[]>([]);
    readonly #dotResourceLinksService = inject(DotResourceLinksService);

    private readonly contentletEffect = effect(() => {
        const contentlet = this.$contentlet();
        if (contentlet) {
            this.content.set(contentlet?.content);
            this.fetchResourceLinks();
        }
    });

    private readonly tempFileEffect = effect(() => {
        const editableImage = this.$editableImage();
        if (editableImage !== undefined) {
            this.isEditable = this.isFileEditable();
        }

        const tempFile = this.$tempFile();
        if (tempFile) {
            this.content.set(tempFile.content);
        }
    });

    get metadata(): DotFileMetadata {
        const tempFile = this.$tempFile();
        const contentlet = this.$contentlet();

        if (tempFile?.metadata) {
            return tempFile.metadata;
        }

        return contentlet ? getFileMetadata(contentlet) : ({} as DotFileMetadata);
    }

    get title(): string {
        const contentlet = this.$contentlet();

        return contentlet?.fileName || this.metadata.name;
    }

    get downloadLink(): string {
        const contentlet = this.$contentlet();
        const fieldVariable = this.$fieldVariable();

        if (!contentlet || !fieldVariable) {
            return '';
        }

        return `/contentAsset/raw-data/${contentlet.inode}/${fieldVariable}?byInode=true&force_download=true`;
    }

    /**
     * Emits event to remove the file
     *
     * @return {*}  {void}
     * @memberof DotBinaryFieldPreviewComponent
     */
    onEdit(): void {
        if (this.$disabled()) {
            return;
        }

        if (this.metadata.editableAsText) {
            this.$editFile.emit();

            return;
        }

        this.$editImage.emit();
    }

    /**
     * fetch the source links for the file
     *
     * @private
     * @memberof DotBinaryFieldPreviewComponent
     */
    private fetchResourceLinks(): void {
        const contentlet = this.$contentlet();
        const fieldVariable = this.$fieldVariable();
        if (!contentlet || !fieldVariable) {
            return;
        }
        this.#dotResourceLinksService
            .getFileResourceLinksByInode({
                fieldVariable,
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

                this.resourceLinks.set([
                    {
                        key: 'FileLink',
                        value: fileLink,
                        show: true
                    },
                    {
                        key: 'Resource-Link',
                        value: text,
                        show: contentlet.baseType === DotCMSBaseTypesContentTypes.FILEASSET
                    },
                    {
                        key: 'VersionPath',
                        value: versionPath,
                        show: true
                    },
                    {
                        key: 'IdPath',
                        value: idPath,
                        show: true
                    }
                ]);
            });
    }

    /**
     * Downloads the file asset
     *
     * @memberof DotBinaryFieldPreviewComponent
     */
    downloadAsset(): void {
        if (this.$disabled()) {
            return;
        }

        window.open(this.downloadLink, '_self');
    }

    /**
     * Check if the file is editable
     *
     * @return {*}  {boolean}
     * @memberof DotBinaryFieldPreviewComponent
     */
    private isFileEditable(): boolean {
        return this.metadata.editableAsText || this.isEditableImage();
    }

    /**
     * Check if the file is an editable image
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotBinaryFieldPreviewComponent
     */
    private isEditableImage(): boolean {
        return this.metadata.isImage && this.$editableImage();
    }
}
