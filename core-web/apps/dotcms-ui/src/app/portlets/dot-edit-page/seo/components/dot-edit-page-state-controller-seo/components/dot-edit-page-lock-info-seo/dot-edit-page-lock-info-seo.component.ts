import { Component, ElementRef, Input, ViewChild } from '@angular/core';

import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotPageRenderState } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-page-lock-info-seo',
    templateUrl: './dot-edit-page-lock-info-seo.component.html',
    styleUrls: ['./dot-edit-page-lock-info-seo.component.scss'],
    imports: [ToggleSwitchModule, DotMessagePipe]
})
export class DotEditPageLockInfoSeoComponent {
    @ViewChild('lockedPageMessage') lockedPageMessage: ElementRef;

    show = false;

    private _state: DotPageRenderState;

    get pageState(): DotPageRenderState {
        return this._state;
    }

    @Input()
    set pageState(value: DotPageRenderState) {
        this._state = value;
        this.show = value.state.lockedByAnotherUser && value.page.canEdit;
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
