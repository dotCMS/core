import { Component, Element, Event, EventEmitter, Prop, Host, h } from '@stencil/core';

/**
 * Represent a dotcms text field for the binary file preview.
 *
 * @export
 * @class DotBinaryFilePreviewComponent
 */
@Component({
    tag: 'dot-binary-file-preview',
    styleUrl: 'dot-binary-file-preview.scss'
})
export class DotBinaryFilePreviewComponent {
    @Element() el: HTMLElement;

    /** file name to be displayed */
    @Prop({ reflect: true, mutable: true })
    fileName = '';

    /** (optional) file URL to be displayed */
    @Prop({ reflect: true, mutable: true })
    previewUrl = '';

    /** (optional) Delete button's label */
    @Prop({ reflect: true })
    deleteLabel = 'Delete';

    /** Emit when the file is deleted */
    @Event() delete: EventEmitter;

    render() {
        return this.fileName ? (
            <Host>
                {this.getPreviewElement()}
                <div class="dot-file-preview__info">
                    <span class="dot-file-preview__name">{this.fileName}</span>
                    <button type="button" onClick={() => this.clearFile()}>
                        {this.deleteLabel}
                    </button>
                </div>
            </Host>
        ) : null;
    }

    private clearFile(): void {
        this.delete.emit();
        this.fileName = null;
        this.previewUrl = null;
    }

    private getPreviewElement() {
        return this.previewUrl ? (
            <img alt={this.fileName} src={this.previewUrl} />
        ) : (
            <div class="dot-file-preview__extension">
                <span>{this.getExtention()}</span>
            </div>
        );
    }

    private getExtention(): string {
        return this.fileName.substr(this.fileName.lastIndexOf('.'));
    }
}
