import { NgStyle } from '@angular/common';
import { Component, inject, Input, OnInit } from '@angular/core';

import { EMPTY_CONTAINER_STYLE_ANGULAR } from '@dotcms/uve/internal';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

@Component({
    selector: 'dotcms-container-not-found',
    standalone: true,
    imports: [NgStyle],
    template: `
        @if ($isDevMode()) {
            <div [attr.data-testid]="'container-not-found'" [ngStyle]="emptyContainerStyle">
                This container with identifier {{ identifier }} was not found.
            </div>
        }
    `
})
export class ContainerNotFoundComponent implements OnInit {
    @Input() identifier = 'unknown';

    #dotcmsContextService = inject(DotCMSStore);

    $isDevMode = this.#dotcmsContextService.$isDevMode;
    emptyContainerStyle = EMPTY_CONTAINER_STYLE_ANGULAR;

    ngOnInit() {
        if (this.$isDevMode()) {
            console.error(`Container with identifier ${this.identifier} not found`);
        }
    }
}
