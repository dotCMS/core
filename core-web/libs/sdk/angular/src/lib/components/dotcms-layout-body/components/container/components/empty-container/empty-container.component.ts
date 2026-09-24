import { NgStyle } from '@angular/common';
import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

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
    // Default, not Eager: Eager does not exist in Angular 19, the oldest version this SDK
    // is built with and supports (libs/sdk/angular/toolchain/README.md). Both mean check-always.
    changeDetection: ChangeDetectionStrategy.Default,
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
