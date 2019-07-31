import { Component, Element, Event, EventEmitter, Prop } from '@stencil/core';
import Fragment from 'stencil-fragment';

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
    @Prop({ reflectToAttr: true, mutable: true })
    fileName = '';

    /** (optional) file URL to be displayed */
    @Prop({ reflectToAttr: true, mutable: true })
    previewUrl = '';

    /** (optional) Delete button's label */
    @Prop({ reflectToAttr: true })
    deleteLabel = 'Delete';

    /** Emit when the file is deleted */
    @Event() delete: EventEmitter;

    render() {
        return this.fileName ? (
            <Fragment>
                {this.getPreviewElement()}
                <div class="dot-file-preview__info">
                    <span class="dot-file-preview__name">{this.fileName}</span>
                    <button type="button" onClick={() => this.clearFile()}>
                        {this.deleteLabel}
                    </button>
                </div>
            </Fragment>
        ) : null;
    }

    private clearFile(): void {
        this.delete.emit();
        this.fileName = null;
        this.previewUrl = null;
    }

    private getPreviewElement(): JSX.Element {
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
