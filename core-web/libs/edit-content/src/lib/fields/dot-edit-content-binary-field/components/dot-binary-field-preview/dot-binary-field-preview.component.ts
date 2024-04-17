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
import { DividerModule } from 'primeng/divider';

import { DotResourceLinks, DotResourceLinksService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';
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

@Component({
    selector: 'dot-binary-field-preview',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DotTempFileThumbnailComponent,
        DotSpinnerModule,
        DialogModule,
        DividerModule,
        DotMessagePipe,
        DotFileSizeFormatPipe,
        DotCopyButtonComponent
    ],
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

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    protected visibility = false;
    protected isEditable = false;
    protected readonly content = signal<string>('');
    protected readonly baseHost = window.location.origin;

    private readonly dotResourceLinksService = inject(DotResourceLinksService);

    get metadata(): DotFileMetadata {
        return this.tempFile?.metadata || getFileMetadata(this.contentlet);
    }

    get title(): string {
        return this.contentlet?.fileName || this.metadata.name;
    }

    protected readonly resourceLinks = signal<DotResourceLinks>(null);

    ngOnInit() {
        this.content.set(this.contentlet?.content);
        this.isEditable = this.metadata.editableAsText || this.isEditableImage();

        if (this.contentlet) {
            this.dotResourceLinksService
                .getFileSourceLinks({
                    fieldVariable: this.fieldVariable,
                    inodeOrIdentifier: this.contentlet.identifier
                })
                .subscribe((resourceLinks) => this.resourceLinks.set(resourceLinks));
        }
    }

    ngOnChanges({ tempFile }: SimpleChanges): void {
        if (tempFile.currentValue) {
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
        if (this.metadata.editableAsText) {
            this.editFile.emit();

            return;
        }

        this.editImage.emit();
    }

    /**
     * Emits event to remove the file
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotBinaryFieldPreviewComponent
     */
    private isEditableImage(): boolean {
        return this.metadata.isImage && this.editableImage;
    }
}
