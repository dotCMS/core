import { Component, Host, h, Prop, Element, Event, EventEmitter, Watch } from '@stencil/core';
import '@material/mwc-button';
import '@material/mwc-icon';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

enum DotFileCurrentStatus {
    UPLOADFILE = 'UploadFile',
    CODEEDITOR = 'CodeEditor',
    FILELIST = 'FileList'
}

@Component({
    tag: 'dot-file-upload',
    styleUrl: 'dot-file-upload.scss'
})
export class DotFileUpload {
    @Element() el: HTMLElement;

    @Prop() currentState: DotFileCurrentStatus = DotFileCurrentStatus.UPLOADFILE;

    @Prop() dropFilesText = 'Drag and Drop or paste a file';
    @Prop() browserButtonText = 'Browser';
    @Prop() writeCodeButtonText = 'Write Code';
    @Prop() cancelButtonText = 'Cancel';

    @Prop({ reflect: true, mutable: true }) assets: DotCMSTempFile[] = [];
    @Watch('assets')
    watchAssets(newAssets: DotCMSTempFile[], _oldAssets: DotCMSTempFile[]) {
        if (newAssets.length > 0 && this.currentState !== DotFileCurrentStatus.FILELIST) {
            this.currentState = DotFileCurrentStatus.FILELIST;
        }
    }

    @Event() fileUploaded: EventEmitter<File[]>;
    @Event() dataChanges: EventEmitter<File[]>;

    private updateCurrentStatus(status: DotFileCurrentStatus) {
        this.currentState = status;
        if (this.currentState !== DotFileCurrentStatus.FILELIST) {
            this.assets = [];
        }
    }

    private fileChangeHandler(event: Event) {
        let files: File[] = [];
        if (
            (event.target as HTMLInputElement).files &&
            (event.target as HTMLInputElement).files.length
        ) {
            Array.from((event.target as HTMLInputElement).files).map((file: File) => {
                files.push(file);
            });
        }
        this.fileUploaded.emit(files);
    }

    private removeAssetFromFileList(assetId) {
        this.assets = this.assets.filter(({ id }: DotCMSTempFile) => id !== assetId);
        if (this.assets.length === 0) {
            this.currentState = DotFileCurrentStatus.UPLOADFILE;
        }
    }
    private getCurrentElement(currentState: DotFileCurrentStatus) {
        if (currentState === DotFileCurrentStatus.UPLOADFILE) {
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
        } else if (currentState === DotFileCurrentStatus.FILELIST) {
            return (
                <div class="dot-file-list">
                    {this.assets.map((asset: DotCMSTempFile) => (
                        <div class="dot-file-list-item">
                            <div>
                                <mwc-icon>insert_drive_file</mwc-icon>
                            </div>
                            <div>{asset.fileName}</div>
                            <div>
                                <mwc-icon onClick={() => this.removeAssetFromFileList(asset.id)}>
                                    delete
                                </mwc-icon>
                            </div>
                        </div>
                    ))}

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
        } else if (currentState === DotFileCurrentStatus.CODEEDITOR) {
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
        if (this.assets.length > 0) {
            this.currentState = DotFileCurrentStatus.FILELIST;
        }
    }

    render() {
        return <Host>{this.getCurrentElement(this.currentState)}</Host>;
    }
}
