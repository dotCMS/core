import { NgStyle } from '@angular/common';
import { Component, inject } from '@angular/core';

import { EMPTY_CONTAINER_STYLE_ANGULAR } from '@dotcms/uve/internal';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

/**
 * @description This component is used to display a message when a container is empty.
 * @export
 * @internal
 * @class EmptyContainerComponent
 */
@Component({
    selector: 'dotcms-empty-container',
    imports: [NgStyle],
    template: `
        @if ($isDevMode()) {
            <div [ngStyle]="emptyContainerStyle" data-testid="empty-container">
                <span data-testid="empty-container-message" data-dot-object="empty-content">
                    This container is empty.
                </span>
            </div>
        }
    `
})
export class EmptyContainerComponent {
    emptyContainerStyle = EMPTY_CONTAINER_STYLE_ANGULAR;

    #dotCMSStore = inject(DotCMSStore);

    $isDevMode = this.#dotCMSStore.$isDevMode;
}
