import { Component, Host, h, Prop, Event, EventEmitter, Watch, State } from '@stencil/core';
import '@material/mwc-button';
import '@material/mwc-icon';
import { DotCMSTempFile, DotUploadFile, DotFileCurrentStatus } from '@dotcms/dotcms-models';

@Component({
    tag: 'dot-file-upload',
    styleUrl: 'dot-file-upload.scss'
})
export class DotFileUpload {
    @State() currentState: DotFileCurrentStatus = DotFileCurrentStatus.UPLOADFILE;

    @Prop() dropFilesText = 'Drag and Drop or paste a file';
    @Prop() browserButtonText = 'Browse';
    @Prop() writeCodeButtonText = 'Write Code';
    @Prop() cancelButtonText = 'Cancel';
    @Prop() accept = '*/*';

    @Prop({ reflect: true, mutable: true }) assets: DotCMSTempFile[] = [];
    @Watch('assets')
    watchAssets(newAssets: DotCMSTempFile[], _oldAssets: DotCMSTempFile[]) {
        // if we have assets then show file list
        if (newAssets.length > 0 && this.currentState !== DotFileCurrentStatus.FILELIST) {
            this.updateCurrentStatus(DotFileCurrentStatus.FILELIST);
        }
    }

    @Event() fileUploaded: EventEmitter<File[]>;
    @Event() dataChanges: EventEmitter<DotUploadFile>;

    private fileInput: HTMLInputElement;

    private updateCurrentStatus(status: DotFileCurrentStatus) {
        this.currentState = status;
        if (this.currentState !== DotFileCurrentStatus.FILELIST) {
            this.assets = [];
        }
        this.dataChanges.emit({ assets: this.assets, currentState: this.currentState });
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

    private removeAssetFromFileList(assetId: string) {
        this.assets = this.assets.filter(({ id }: DotCMSTempFile) => id !== assetId);
        if (this.assets.length === 0) {
            this.updateCurrentStatus(DotFileCurrentStatus.UPLOADFILE);
        }
    }

    private browserFile() {
        this.fileInput.click();
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
                            accept={this.accept}
                            ref={(elem: HTMLInputElement) => (this.fileInput = elem)}
                        />
                        <mwc-icon>insert_drive_file</mwc-icon>
                        <p>{this.dropFilesText}</p>
                    </div>
                    <div class="dot-file-upload-actions">
                        <mwc-button onClick={() => this.browserFile()} outlined>
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

    componentWillLoad(): void {
        if (this.assets.length > 0) {
            this.updateCurrentStatus(DotFileCurrentStatus.FILELIST);
        }
    }

    render() {
        return <Host>{this.getCurrentElement(this.currentState)}</Host>;
    }
}
