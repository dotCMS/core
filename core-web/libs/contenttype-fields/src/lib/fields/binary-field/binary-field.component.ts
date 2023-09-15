import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { AsyncPipe, NgClass, NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
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

import { filter } from 'rxjs/operators';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotDropZoneMessageComponent,
    DotMessagePipe,
    DropZoneFileEvent
} from '@dotcms/ui';

import { BINARY_FIELD_MODE, DotBinaryFieldStore } from './store/binary-field.store';

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [
        NgIf,
        NgClass,
        NgSwitch,
        NgSwitchCase,
        AsyncPipe,
        ButtonModule,
        DialogModule,
        DotDropZoneComponent,
        MonacoEditorModule,
        DotMessagePipe,
        DotDropZoneMessageComponent
    ],
    providers: [DotBinaryFieldStore],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldComponent implements OnInit {
    //Inputs
    @Input() accept: string[] = [];
    @Input() maxFileSize: number;
    @Input() helperText: string;

    @ViewChild('inputFile') inputFile: ElementRef;

    @Output() tempFile = new EventEmitter<DotCMSTempFile>();
    @Output() tempId = new EventEmitter<string>();

    readonly dialogHeaderMap = {
        [BINARY_FIELD_MODE.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BINARY_FIELD_MODE.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BINARY_FIELD_MODE = BINARY_FIELD_MODE;
    readonly mode$ = this.dotBinaryFieldStore.mode$;
    readonly vm$ = this.dotBinaryFieldStore.state$;

    dialogOpen = false;
    dropZoneActive = false;

    constructor(private readonly dotBinaryFieldStore: DotBinaryFieldStore) {}

    ngOnInit() {
        this.dotBinaryFieldStore.tempFile$
            .pipe(filter((tempFile) => !!tempFile))
            .subscribe((tempFile) => this.tempFile.emit(tempFile));

        this.dotBinaryFieldStore.setRules({
            accept: this.accept,
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
        this.dropZoneActive = value;
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
        this.dotBinaryFieldStore.setMode(mode);
        this.dialogOpen = true;
    }

    /**
     * Open file picker dialog
     *
     * @memberof DotBinaryFieldComponent
     */
    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    handleFileSelection(_event) {
        // TODO: Implement - Chose File
    }

    handleCreateFile(_event) {
        // TODO: Implement - Write Code
    }

    handleExternalSourceFile(_event) {
        // TODO: Implement - FROM URL
    }
}
