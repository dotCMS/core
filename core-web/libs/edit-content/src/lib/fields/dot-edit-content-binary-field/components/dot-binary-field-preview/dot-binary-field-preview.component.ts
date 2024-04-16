import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    inject
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';

import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';
import {
    DotTempFileThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotSpinnerModule,
    DotCopyButtonComponent
} from '@dotcms/ui';

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
export class DotBinaryFieldPreviewComponent implements OnChanges {
    @Input() contentlet: DotCMSContentlet;
    @Input() tempFile: DotCMSTempFile;
    @Input() editableImage: boolean;

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    visibility = false;

    private httpClient = inject(HttpClient);

    protected isEditable = false;
    protected readonly baseHost = window.location.origin;

    get content(): string {
        return this.tempFile?.content || this.contentlet?.content;
    }

    get metadata(): DotFileMetadata {
        return this.tempFile?.metadata || this.contentletMetadata;
    }

    get title(): string {
        return this.contentlet?.fileName || this.metadata.name;
    }

    get contentletMetadata(): DotFileMetadata {
        const { metaData = '', fieldVariable = '' } = this.contentlet;

        return metaData || this.contentlet[`${fieldVariable}MetaData`];
    }

    get objectFit(): string {
        if (this.metadata?.height > this.metadata?.width) {
            return 'contain';
        }

        return 'cover';
    }

    ngOnChanges(): void {
        this.setIsEditable();
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

    private setIsEditable() {
        this.isEditable =
            this.metadata.editableAsText || (this.metadata.isImage && this.editableImage);
    }
}
