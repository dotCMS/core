import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import { DotClipboardUtil } from '../../services/clipboard/ClipboardUtil';

/**
 * Icon button to copy to clipboard the string you pass to it,
 * it uses tooltip to indicate the user the action and the result.
 *
 * @export
 * @class DotCopyButtonComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-copy-button',
    standalone: true,
    providers: [DotClipboardUtil],
    templateUrl: './dot-copy-button.component.html',
    styleUrls: ['./dot-copy-button.component.scss'],
    imports: [TooltipModule, ButtonModule, NgIf],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCopyButtonComponent implements OnInit {
    @Input() copy = '';
    @Input() label: string;
    @Input() tooltipText: string;

    constructor(
        private dotClipboardUtil: DotClipboardUtil,
        private dotMessageService: DotMessageService,
        private readonly cd: ChangeDetectorRef
    ) {}

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
                    this.cd.detectChanges();
                }, 1000);
            })
            .catch(() => {
                this.tooltipText = 'Error';
                this.cd.detectChanges();
            });
    }
}
