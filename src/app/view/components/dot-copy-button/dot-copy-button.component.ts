import { Component, OnInit, Input } from '@angular/core';
import { DotClipboardUtil } from 'src/app/api/util/clipboard/ClipboardUtil';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-copy-button',
    templateUrl: './dot-copy-button.component.html',
    styleUrls: ['./dot-copy-button.component.scss']
})
export class DotCopyButtonComponent implements OnInit {
    @Input() copy = '';

    i18nMessages: {
        [key: string]: string;
    } = {};

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
        this.dotMessageService
            .getMessages(['Copy', 'Copied'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.i18nMessages = messages;

                if (!this.tooltipText) {
                    this.tooltipText = this.tooltipText || this.i18nMessages['Copy'];
                }
            });
    }

    /**
     * Copy url to clipboard
     *
     * @memberof DotCopyButtonComponent
     */
    copyUrlToClipboard(): void {
        this.dotClipboardUtil
            .copy(this.copy)
            .then(() => {
                const original = this.tooltipText;
                this.tooltipText = this.i18nMessages['Copied'];

                setTimeout(() => {
                    this.tooltipText = original;
                }, 1000);
            })
            .catch(() => {
                this.tooltipText = 'Error';
            });
    }
}
