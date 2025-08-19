import { Component, ElementRef, Input, ViewChild } from '@angular/core';

import { DotPageRenderState } from '@dotcms/dotcms-models';

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
    styleUrls: ['./dot-edit-page-lock-info.component.scss'],
    standalone: false
})
export class DotEditPageLockInfoComponent {
    @ViewChild('lockedPageMessage') lockedPageMessage: ElementRef;

    show = false;

    private _state: DotPageRenderState;

    @Input()
    set pageState(value: DotPageRenderState) {
        this._state = value;
        this.show = value.state.lockedByAnotherUser && value.page.canEdit;
    }

    get pageState(): DotPageRenderState {
        return this._state;
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
