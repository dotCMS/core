import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { delay, filter, skip, tap } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerModule,
    DropZoneErrorType,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DotBinaryFieldEditorComponent } from './components/dot-binary-field-editor/dot-binary-field-editor.component';
import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotBinaryFieldUrlModeComponent } from './components/dot-binary-field-url-mode/dot-binary-field-url-mode.component';
import { BinaryFieldMode, BinaryFieldStatus } from './interfaces';
import { DotBinaryFieldEditImageService } from './service/dot-binary-field-edit-image/dot-binary-field-edit-image.service';
import { DotBinaryFieldStore } from './store/binary-field.store';

import { getUiMessage } from '../../utils/binary-field-utils';

@Component({
    selector: 'dot-binary-field',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DialogModule,
        DotDropZoneComponent,
        MonacoEditorModule,
        DotMessagePipe,
        DotBinaryFieldUiMessageComponent,
        DotSpinnerModule,
        HttpClientModule,
        DotBinaryFieldEditorComponent,
        InputTextModule,
        DotBinaryFieldUrlModeComponent,
        DotBinaryFieldPreviewComponent
    ],
    providers: [DotBinaryFieldStore, DotLicenseService, DotBinaryFieldEditImageService],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldComponent implements OnInit, AfterViewInit {
    @Input() field: DotCMSContentTypeField;
    @Input() contentlet: DotCMSContentlet;
    @Output() tempFile = new EventEmitter<DotCMSTempFile>();
    @ViewChild('inputFile') inputFile: ElementRef;

    readonly dialogHeaderMap = {
        [BinaryFieldMode.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BinaryFieldMode.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BinaryFieldStatus = BinaryFieldStatus;
    readonly BinaryFieldMode = BinaryFieldMode;
    readonly vm$ = this.dotBinaryFieldStore.vm$;

    private tempId = '';
    dialogOpen = false;
    accept: string[] = [];
    maxFileSize: number;
    helperText: string;

    constructor(
        private readonly dotBinaryFieldStore: DotBinaryFieldStore,
        private readonly dotMessageService: DotMessageService,
        private readonly dotBinaryFieldEditImageService: DotBinaryFieldEditImageService,
        private readonly cd: ChangeDetectorRef
    ) {
        this.dotMessageService.init();
    }

    ngOnInit() {
        this.dotBinaryFieldStore.tempFile$.pipe(skip(1)).subscribe((tempFile) => {
            this.tempId = tempFile?.id;
            this.tempFile.emit(tempFile);
        });

        this.dotBinaryFieldEditImageService
            .editedImage()
            .pipe(
                filter((tempFile) => !!tempFile),
                tap(() => this.dotBinaryFieldStore.setStatus(BinaryFieldStatus.UPLOADING)),
                delay(500) // Loading animation
            )
            .subscribe((tempFile) => {
                this.setTempFile(tempFile);
            });

        this.dotBinaryFieldStore.setMaxFileSize(this.maxFileSize);
    }

    ngAfterViewInit() {
        this.setFieldVariables();
        if (this.contentlet) {
            this.setPreviewFile();
        }

        this.cd.detectChanges();
    }

    openDialog(mode: BinaryFieldMode) {
        this.dialogOpen = true;
        this.dotBinaryFieldStore.setMode(mode);
    }

    closeDialog() {
        this.dialogOpen = false;
    }

    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    handleFileSelection(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files[0];
        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    removeFile() {
        this.dotBinaryFieldStore.removeFile();
    }

    setTempFile(tempFile: DotCMSTempFile) {
        this.dotBinaryFieldStore.setTempFile(tempFile);
        this.closeDialog();
    }

    onEditFile() {
        this.openDialog(BinaryFieldMode.EDITOR);
    }

    onEditImage() {
        this.dotBinaryFieldEditImageService.openImageEditor({
            inode: this.contentlet?.inode,
            tempId: this.tempId,
            variable: this.field.variable
        });
    }

    setDropZoneActiveState(value: boolean) {
        this.dotBinaryFieldStore.setDropZoneActive(value);
    }

    handleFileDrop({ validity, file }: DropZoneFileEvent) {
        if (!validity.valid) {
            this.handleFileDropError(validity);

            return;
        }

        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    private setPreviewFile() {
        const variable = this.field.variable;
        const metaDataKey = variable + 'MetaData';
        const { titleImage, inode, [metaDataKey]: metadata } = this.contentlet;
        const { contentType: mimeType } = metadata;

        this.dotBinaryFieldStore.setFileAndContent({
            inode,
            titleImage,
            mimeType,
            url: this.contentlet[variable],
            ...metadata
        });
    }

    private setFieldVariables() {
        const { accept, maxFileSize = 0, helperText } = this.getFieldVariables();
        this.accept = accept ? accept.split(',') : [];
        this.maxFileSize = Number(maxFileSize);
        this.helperText = helperText;
    }

    private getFieldVariables(): Record<string, string> {
        return this.field.fieldVariables.reduce(
            (prev, { key, value }) => ({
                ...prev,
                [key]: value
            }),
            {}
        );
    }

    private handleFileDropError({ errorsType }: DropZoneFileValidity): void {
        const messageArgs = {
            [DropZoneErrorType.FILE_TYPE_MISMATCH]: this.accept.join(', '),
            [DropZoneErrorType.MAX_FILE_SIZE_EXCEEDED]: `${this.maxFileSize} bytes`
        };
        const errorType = errorsType[0];
        const uiMessage = getUiMessage(errorType, messageArgs[errorType]);

        this.dotBinaryFieldStore.invalidFile(uiMessage);
    }
}
