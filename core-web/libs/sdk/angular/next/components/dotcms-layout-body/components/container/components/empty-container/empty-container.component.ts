import { NgStyle } from '@angular/common';
import { Component, inject, Input, signal } from '@angular/core';

import { EMPTY_CONTAINER_STYLE } from '@dotcms/uve/internal';
import { DotContainerAttributes } from '@dotcms/uve/types';

import { DotCMSContextService } from '../../../../../../services/dotcms-context/dotcms-context.service';

@Component({
    selector: 'dotcms-empty-container',
    standalone: true,
    imports: [NgStyle],
    template: `
        @if (isDevMode()) {
            <div
                [ngStyle]="emptyContainerStyle"
                [attr.data-dot-object]="dotAttributes['data-dot-object']"
                [attr.data-dot-identifier]="dotAttributes['data-dot-identifier']"
                [attr.data-dot-accept-types]="dotAttributes['data-dot-accept-types']"
                [attr.data-max-contentlets]="dotAttributes['data-max-contentlets']"
                [attr.data-dot-uuid]="dotAttributes['data-dot-uuid']">
                <span data-testid="empty-container-message">This container is empty.</span>
            </div>
        }
    `
})
export class EmptyContainerComponent {
    @Input() dotAttributes: DotContainerAttributes = {} as DotContainerAttributes;

    emptyContainerStyle = EMPTY_CONTAINER_STYLE;

    #dotcmsContextService = inject(DotCMSContextService);

    isDevMode = signal(false);

    ngOnInit() {
        this.isDevMode.set(this.#dotcmsContextService.isDevMode());
    }
}
