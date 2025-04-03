import { NgStyle } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { EMPTY_CONTAINER_STYLE } from '@dotcms/uve/internal';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

@Component({
    selector: 'dotcms-empty-container',
    standalone: true,
    imports: [NgStyle],
    template: `
        @if ($isDevMode()) {
            <div [ngStyle]="emptyContainerStyle">
                <span data-testid="empty-container-message">This container is empty.</span>
            </div>
        }
    `
})
export class EmptyContainerComponent {
    emptyContainerStyle = EMPTY_CONTAINER_STYLE;

    #dotCMSStore = inject(DotCMSStore);

    $isDevMode = signal(false);

    ngOnInit() {
        this.$isDevMode.set(this.#dotCMSStore.isDevMode());
    }
}
