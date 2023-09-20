import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
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

import { skip } from 'rxjs/operators';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerModule,
    DropZoneFileEvent
} from '@dotcms/ui';

import { DotUiMessageComponent } from './components/dot-ui-message/dot-ui-message.component';
import {
    BINARY_FIELD_MODE,
    BINARY_FIELD_STATUS,
    DotBinaryFieldStore
} from './store/binary-field.store';

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
        DotUiMessageComponent,
        DotSpinnerModule,
        HttpClientModule
    ],
    providers: [DotBinaryFieldStore, DotMessageService, DotUploadService],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldComponent implements OnInit {
    //Inputs
    private _accept: string[] = [];
    @Input() set accept(accept: string) {
        this._accept = accept.split(',').map((type) => type.trim());
    }

    @Input() maxFileSize: number;
    @Input() helperText: string;

    @ViewChild('inputFile') inputFile: ElementRef;

    @Output() tempFile = new EventEmitter<DotCMSTempFile>();

    readonly dialogHeaderMap = {
        [BINARY_FIELD_MODE.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BINARY_FIELD_MODE.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BINARY_FIELD_STATUS = BINARY_FIELD_STATUS;
    readonly BINARY_FIELD_MODE = BINARY_FIELD_MODE;
    readonly vm$ = this.dotBinaryFieldStore.vm$;

    constructor(
        private readonly dotBinaryFieldStore: DotBinaryFieldStore,
        private readonly dotMessageService: DotMessageService
    ) {
        this.dotMessageService.init();
    }

    ngOnInit() {
        this.dotBinaryFieldStore.tempFile$
            .pipe(skip(1)) // Skip initial state
            .subscribe((tempFile) => {
                this.tempFile.emit(tempFile);
            });

        this.dotBinaryFieldStore.setRules({
            accept: this._accept,
            maxFileSize: this.maxFileSize
        });
    }

    /**
     *  Set drop zone active state
     *
     * @param {boolean} value
     * @memberof DotBinaryFieldComponent
     */
    setDropZoneActiveState(value: boolean) {
        this.dotBinaryFieldStore.setDropZoneActive(value);
    }

    /**
     * Handle file dropped
     *
     * @param {DropZoneFileEvent} { validity }
     * @return {*}
     * @memberof BinaryFieldComponent
     */
    handleFileDrop(event: DropZoneFileEvent) {
        this.setDropZoneActiveState(false);
        this.dotBinaryFieldStore.handleFileDrop(event);
    }

    /**
     * Open dialog
     *
     * @param {BINARY_FIELD_MODE} mode
     * @memberof DotBinaryFieldComponent
     */
    openDialog(mode: BINARY_FIELD_MODE) {
        this.dotBinaryFieldStore.openDialog(mode);
    }

    /**
     * Listen to dialog visibility change
     * and set mode to dropzone when dialog is closed
     *
     * @param {boolean} visibily
     * @memberof DotBinaryFieldComponent
     */
    visibleChange(visibily: boolean) {
        if (!visibily) {
            this.dotBinaryFieldStore.closeDialog();
        }
    }

    /**
     * Open file picker dialog
     *
     * @memberof DotBinaryFieldComponent
     */
    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    /**
     * Handle file selection
     *
     * @param {Event} event
     * @memberof DotBinaryFieldComponent
     */
    handleFileSelection(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files[0];
        this.dotBinaryFieldStore.handleFileSelection(file);
    }

    /**
     * Remove file
     *
     * @memberof DotBinaryFieldComponent
     */
    removeFile() {
        this.dotBinaryFieldStore.removeFile();
    }

    handleCreateFile(_event) {
        // TODO: Implement - Write Code
    }

    handleExternalSourceFile(_event) {
        // TODO: Implement - FROM URL
    }
}
