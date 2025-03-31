import { NgStyle } from '@angular/common';
import { Component, inject, Input, signal } from '@angular/core';

import { EMPTY_CONTAINER_STYLE } from '@dotcms/uve/internal';

import { DotCMSContextService } from '../../../../../../services/dotcms-context/dotcms-context.service';

@Component({
    selector: 'dot-empty-container',
    standalone: true,
    imports: [NgStyle],
    template: `
        @if (isDevMode()) {
            <div
                [ngStyle]="emptyContainerStyle"
                [attr.data-dot-object]="dotAttributes['data-dot-object']"
                [attr.data-dot-inode]="dotAttributes['data-dot-inode']"
                [attr.data-dot-identifier]="dotAttributes['data-dot-identifier']">
                <span data-testid="empty-container-message">This container is empty.</span>
            </div>
        }
    `
})
export class EmptyContainerComponent {
    @Input() dotAttributes: { [key: string]: string } = {};

    emptyContainerStyle = EMPTY_CONTAINER_STYLE;

    #dotcmsContextService = inject(DotCMSContextService);

    isDevMode = signal(false);

    ngOnInit() {
        this.isDevMode.set(this.#dotcmsContextService.isDevMode());
    }
}
