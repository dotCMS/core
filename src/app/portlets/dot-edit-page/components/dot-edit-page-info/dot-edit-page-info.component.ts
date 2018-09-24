import { Component, OnInit, Input, ElementRef, ViewChild } from '@angular/core';

import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotClipboardUtil } from '../../../../api/util/clipboard/ClipboardUtil';
import { SiteService } from 'dotcms-js/dotcms-js';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-page-info',
    templateUrl: './dot-edit-page-info.component.html',
    styleUrls: ['./dot-edit-page-info.component.scss']
})
export class DotEditPageInfoComponent implements OnInit {
    @Input()
    pageState: DotRenderedPageState;
    @ViewChild('lockedPageMessage')
    lockedPageMessage: ElementRef;

    constructor(
        private dotClipboardUtil: DotClipboardUtil,
        private dotGlobalMessageService: DotGlobalMessageService,
        private siteService: SiteService,
        public dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'dot.common.message.pageurl.copied.clipboard',
                'dot.common.message.pageurl.copied.clipboard.error',
                'editpage.toolbar.page.cant.edit',
                'editpage.toolbar.page.locked.by.user'
            ])
            .subscribe();
    }

    /**
     * Make the lock message blink with css
     *
     * @memberof DotEditPageInfoComponent
     */
    blinkLockMessage(): void {
        const blinkClass = 'page-info__locked-by-message--blink';

        this.lockedPageMessage.nativeElement.classList.add(blinkClass);
        setTimeout(() => {
            this.lockedPageMessage.nativeElement.classList.remove(blinkClass);
        }, 500);
    }

    /**
     * Copy url to clipboard
     *
     * @returns {boolean}
     * @memberof DotEditPageToolbarComponent
     */
    copyUrlToClipboard(): void {
        this.dotClipboardUtil
            .copy(this.getFullUrl(this.pageState.page.pageURI))
            .then(() => {
                this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard'));
            })
            .catch(() => {
                this.dotGlobalMessageService.error(this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard.error'));
            });
    }

    private getFullUrl(url: string): string {
        return [location.protocol, '//', this.siteService.currentSite['name'], location.port ? `:${location.port}` : '', url].join('');
    }
}
