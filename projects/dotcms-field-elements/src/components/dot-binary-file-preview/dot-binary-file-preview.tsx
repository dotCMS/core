import { Component, Element, Event, EventEmitter, Prop } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotTempFile } from '../../models';

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

    /** (optional) file to be displayed */
    @Prop({ reflectToAttr: true, mutable: true })
    file: DotTempFile = null;

    /** (optional) Delete button's label */
    @Prop({ reflectToAttr: true })
    deleteLabel = 'Delete';

    /** Emit when the file is deleted */
    @Event() delete: EventEmitter;

    render() {
        return this.file ? (
            <Fragment>
                {this.getPreviewElement()}
                <div class="dot-file-preview__info">
                    <span class="dot-file-preview__name">
                        {this.file ? this.file.fileName : ''}
                    </span>
                    <button onClick={() => this.clearFile()}>{this.deleteLabel}</button>
                </div>
            </Fragment>
        ) : null;
    }

    private clearFile(): void {
        this.delete.emit(this.file);
        this.file = null;
    }

    private getPreviewElement(): JSX.Element {
        return this.file ? (
            this.file.image ? (
                <img src={this.file.thumbnailUrl} />
            ) : (
                <div class="dot-file-preview__extension">
                    <span>{this.getExtention()}</span>
                </div>
            )
        ) : null;
    }

    private getExtention(): string {
        return this.file.fileName.substr(this.file.fileName.lastIndexOf('.'));
    }
}
