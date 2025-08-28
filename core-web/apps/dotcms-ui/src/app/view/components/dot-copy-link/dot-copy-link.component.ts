import { Component, Input, OnInit, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil } from '@dotcms/ui';

/**
 * Icon button to copy to clipboard the string you pass to it,
 * it use tooltip to indicate the user the action and the result.
 *
 * @export
 * @class DotCopyLinkComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-copy-link',
    templateUrl: './dot-copy-link.component.html',
    styleUrls: ['./dot-copy-link.component.scss'],
    standalone: false
})
export class DotCopyLinkComponent implements OnInit {
    private dotClipboardUtil = inject(DotClipboardUtil);
    private dotMessageService = inject(DotMessageService);

    @Input() copy = '';
    @Input() label: string;
    @Input() tooltipText: string;

    ngOnInit() {
        this.tooltipText = this.tooltipText || this.dotMessageService.get('Copy');
        this.label = this.label || this.dotMessageService.get('Copy');
    }

    /**
     * Copy url to clipboard and update the tooltip text with the result
     *
     * @param {MouseEvent} $event
     * @memberof DotCopyLinkComponent
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
