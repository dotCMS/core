import { Component, OnInit, Input } from '@angular/core';
import { DotClipboardUtil } from 'src/app/api/util/clipboard/ClipboardUtil';

@Component({
    selector: 'dot-copy-button',
    templateUrl: './dot-copy-button.component.html',
    styleUrls: ['./dot-copy-button.component.scss']
})
export class DotCopyButtonComponent implements OnInit {
    @Input() copy = 'Hello World';

    @Input() tooltipText = 'Copy';

    constructor(private dotClipboardUtil: DotClipboardUtil) {}

    ngOnInit() {}

    /**
     * Copy url to clipboard
     *
     * @returns boolean
     * @memberof DotEditPageToolbarComponent
     */
    copyUrlToClipboard($event: MouseEvent): void {
        $event.preventDefault();
        $event.stopPropagation();
        this.dotClipboardUtil
            .copy(this.copy)
            .then(() => {
                this.tooltipText = 'Copied';

                setTimeout(() => {
                    this.tooltipText = 'Copy';
                }, 1000);
            })
            .catch(() => {
                this.tooltipText = 'Error';
            });
    }
}
