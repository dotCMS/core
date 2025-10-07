import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    inject,
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
    DotTempFileThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotSpinnerModule,
    DotCopyButtonComponent
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
        DotSpinnerModule,
        DialogModule,
        DotMessagePipe,
        DotFileSizeFormatPipe,
        DotCopyButtonComponent
    ],
    providers: [DotResourceLinksService],
    templateUrl: './dot-binary-field-preview.component.html',
    styleUrls: ['./dot-binary-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotBinaryFieldPreviewComponent implements OnInit, OnChanges {
    @Input() contentlet: DotCMSContentlet;
    @Input() tempFile: DotCMSTempFile;
    @Input() editableImage: boolean;
    @Input() fieldVariable: string;
    @Input() disabled = false;

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    protected visibility = false;
    protected isEditable = false;
    protected readonly content = signal<string>('');
    protected readonly resourceLinks = signal<dotPreviewResourceLink[]>([]);
    readonly #dotResourceLinksService = inject(DotResourceLinksService);

    get metadata(): DotFileMetadata {
        return this.tempFile?.metadata ?? getFileMetadata(this.contentlet);
    }

    get title(): string {
        return this.contentlet?.fileName || this.metadata.name;
    }

    get downloadLink(): string {
        return `/contentAsset/raw-data/${this.contentlet.inode}/${this.fieldVariable}?byInode=true&force_download=true`;
    }

    ngOnInit() {
        if (this.contentlet) {
            this.content.set(this.contentlet?.content);
            this.fetchResourceLinks();
        }
    }

    ngOnChanges({ tempFile, editableImage }: SimpleChanges): void {
        if (editableImage) {
            this.isEditable = this.isFileEditable();
        }

        if (tempFile?.currentValue) {
            this.content.set(tempFile.currentValue.content);
        }
    }

    /**
     * Emits event to remove the file
     *
     * @return {*}  {void}
     * @memberof DotBinaryFieldPreviewComponent
     */
    onEdit(): void {
        if (this.disabled) {
            return;
        }

        if (this.metadata.editableAsText) {
            this.editFile.emit();

            return;
        }

        this.editImage.emit();
    }

    /**
     * fetch the source links for the file
     *
     * @private
     * @memberof DotBinaryFieldPreviewComponent
     */
    private fetchResourceLinks(): void {
        this.#dotResourceLinksService
            .getFileResourceLinksByInode({
                fieldVariable: this.fieldVariable,
                inode: this.contentlet.inode
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
                        show: this.contentlet.baseType === DotCMSBaseTypesContentTypes.FILEASSET
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
        if (this.disabled) {
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
        return this.metadata.isImage && this.editableImage;
    }
}
