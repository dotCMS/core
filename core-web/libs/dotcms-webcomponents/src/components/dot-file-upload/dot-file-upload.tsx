import { Component, Host, State, h, Prop, Element, Event, EventEmitter } from '@stencil/core';
import '@material/mwc-button';
import '@material/mwc-icon';

enum DotFileCurrentStatus {
    UPLOADFILE = 'UploadFile',
    CODEEDITOR = 'CodeEditor',
    FileList = 'FileList'
}

@Component({
    tag: 'dot-file-upload',
    styleUrl: 'dot-file-upload.scss'
})
export class DotFileUpload {
    @Element() el: HTMLElement;

    @State() currentState: DotFileCurrentStatus = DotFileCurrentStatus.UPLOADFILE;

    @Prop() dropFilesText = 'Drag and Drop or paste a file';
    @Prop() browserButtonText = 'Browser';
    @Prop() writeCodeButtonText = 'Write Code';
    @Prop() cancelButtonText = 'Cancel';

    @Event() fileUploaded: EventEmitter;

    updateCurrentStatus(status: DotFileCurrentStatus) {
        this.currentState = status;
    }

    fileChangeHandler(event: Event) {
        this.fileUploaded.emit(event);
    }
    getCurrentElement() {
        if (this.currentState === DotFileCurrentStatus.UPLOADFILE) {
            return (
                <div class="dot-file-upload">
                    <div class="dot-file-upload-drop-message">
                        <input
                            type="file"
                            name="dot-file-upload-file"
                            onChange={(event: Event) => this.fileChangeHandler(event)}
                        />
                        <mwc-icon class="material-icons-outlined">insert_drive_file</mwc-icon>
                        <p>{this.dropFilesText}</p>
                    </div>
                    <div class="dot-file-upload-actions">
                        <mwc-button onClick={() => this.fileInput.click()} outlined>
                            {this.browserButtonText}
                        </mwc-button>
                        <mwc-button
                            onClick={() =>
                                this.updateCurrentStatus(DotFileCurrentStatus.CODEEDITOR)
                            }
                            outlined
                        >
                            {this.writeCodeButtonText}
                        </mwc-button>
                    </div>
                </div>
            );
        } else if (this.currentState === DotFileCurrentStatus.FileList) {
            return (
                <div class="dot-file-list">
                    <div class="dot-file-list-item">
                        <div>
                            <mwc-icon>insert_drive_file</mwc-icon>
                        </div>
                        <div>File12345</div>
                        <div>
                            <mwc-icon>delete</mwc-icon>
                        </div>
                    </div>

                    <div class="dot-file-list-cancel-box">
                        <mwc-button
                            onClick={() =>
                                this.updateCurrentStatus(DotFileCurrentStatus.UPLOADFILE)
                            }
                            outlined
                        >
                            {this.cancelButtonText}
                        </mwc-button>
                    </div>
                </div>
            );
        } else if (this.currentState === DotFileCurrentStatus.CODEEDITOR) {
            return (
                <div class="dot-file-editor">
                    <slot />
                    <div class="dot-file-editor-cancel-box">
                        <mwc-button
                            onClick={() =>
                                this.updateCurrentStatus(DotFileCurrentStatus.UPLOADFILE)
                            }
                            outlined
                        >
                            {this.cancelButtonText}
                        </mwc-button>
                    </div>
                </div>
            );
        } else {
            return (
                <div class="dot-file-upload">
                    <div class="dot-file-upload-drop-message">
                        <mwc-icon>insert_drive_file</mwc-icon>
                        <p>{this.dropFilesText}</p>
                    </div>
                    <div class="dot-file-upload-actions">
                        <mwc-button outlined>{this.browserButtonText}</mwc-button>
                        <mwc-button outlined>{this.writeCodeButtonText}</mwc-button>
                    </div>
                </div>
            );
        }
    }

    private fileInput: HTMLInputElement;

    componentDidLoad(): void {
        this.fileInput = this.el.querySelector('.dot-file-upload input');
    }

    render() {
        return <Host>{this.getCurrentElement()}</Host>;
    }
}
