import { Component, Input, OnInit } from '@angular/core';

import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { DotMessageService } from '@dotcms/data-access';

@Component({
    selector: 'dot-copy-content',
    templateUrl: './dot-copy-content.component.html',
    styleUrls: ['./dot-copy-content.component.scss']
})
export class DotCopyContentComponent implements OnInit {
    @Input() copy = '';
    @Input() label: string;
    @Input() tooltipText: string;

    constructor(
        private dotClipboardUtil: DotClipboardUtil,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.tooltipText = this.tooltipText || this.dotMessageService.get('Copy');
        this.label = this.label || this.dotMessageService.get('Copy');
    }

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
