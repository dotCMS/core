import { Component, OnInit, Input } from '@angular/core';
import { DotClipboardUtil } from 'src/app/api/util/clipboard/ClipboardUtil';
import { DotMessageService } from '@services/dot-messages-service';

/**
 * Icon button to copy to clipboard the string you pass to it,
 * it use tooltip to indicate the user the action and the result.
 *
 * @export
 * @class DotCopyButtonComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-copy-button',
    templateUrl: './dot-copy-button.component.html',
    styleUrls: ['./dot-copy-button.component.scss']
})
export class DotCopyButtonComponent implements OnInit {
    @Input() copy = '';
    @Input() label: string;

    private _tooltipText: string;

    constructor(
        private dotClipboardUtil: DotClipboardUtil,
        private dotMessageService: DotMessageService
    ) {}

    get tooltipText(): string {
        return this._tooltipText;
    }

    @Input('tooltipText')
    set tooltipText(value: string) {
        this._tooltipText = value;
    }

    ngOnInit() {
        this.tooltipText = this.tooltipText || this.dotMessageService.get('Copy');
    }

    /**
     * Copy url to clipboard and update the tooltip text with the result
     *
     * @memberof DotCopyButtonComponent
     */
    copyUrlToClipboard($event: MouseEvent): void {
        $event.stopPropagation();

        this.dotClipboardUtil
            .copy(this.copy)
            .then(() => {
                const original = this.tooltipText;
                this.tooltipText = this.dotMessageService.get('Copied');

                setTimeout(() => {
                    this.tooltipText = original;
                }, 1000);
            })
            .catch(() => {
                this.tooltipText = 'Error';
            });
    }
}
