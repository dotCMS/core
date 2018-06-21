import { Component, OnInit, Input, ElementRef, ViewChild } from '@angular/core';
import { DotRenderedPageState } from '../../../../../shared/models/dot-rendered-page-state.model';
import { DotMessageService } from '../../../../../../../api/services/dot-messages-service';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-page-lock-info',
    templateUrl: './dot-edit-page-lock-info.component.html',
    styleUrls: ['./dot-edit-page-lock-info.component.scss']
})
export class DotEditPageLockInfoComponent implements OnInit {
    @Input() pageState: DotRenderedPageState;
    @ViewChild('lockedPageMessage') lockedPageMessage: ElementRef;

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
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
}
