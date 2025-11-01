import { Observable } from 'rxjs';

import { animate, AnimationEvent, state, style, transition, trigger } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { Component, inject, Input, ViewChild } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotPaletteContentTypeComponent } from './dot-palette-content-type/dot-palette-content-type.component';
import { DotPaletteContentletsComponent } from './dot-palette-contentlets/dot-palette-contentlets.component';
import { DotPaletteState, DotPaletteStore } from './store/dot-palette.store';

@Component({
    selector: 'dot-palette',
    templateUrl: './dot-palette.component.html',
    styleUrls: ['./dot-palette.component.scss'],
    providers: [DotPaletteStore],
    imports: [CommonModule, DotPaletteContentTypeComponent, DotPaletteContentletsComponent],
    animations: [
        trigger('inOut', [
            state(
                'contentlet:in',
                style({
                    transform: 'translateX(-100%)'
                })
            ),
            state(
                'contentlet:out',
                style({
                    transform: 'translateX(0%)'
                })
            ),
            transition('* => *', animate('200ms ease-in'))
        ])
    ]
})
export class DotPaletteComponent {
    readonly #store = inject(DotPaletteStore);

    @Input() set allowedContent(items: string[]) {
        this.#store.setAllowedContent(items);
    }
    @Input() set languageId(languageId: string) {
        this.#store.switchLanguage(languageId);
    }
    vm$: Observable<DotPaletteState> = this.#store.vm$;

    @ViewChild('contentlets') contentlets: DotPaletteContentletsComponent;
    @ViewChild('contentTypes') contentTypes: DotPaletteContentTypeComponent;

    /**
     * Sets value on store to show/hide components on the UI
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        this.#store.switchView(variableName);
    }

    /**
     * Event to filter contentlets data on the store
     *
     * @param {string} value
     * @memberof DotPaletteComponent
     */
    filterContentlets(value: string): void {
        this.#store.filterContentlets(value);
    }

    /**
     * Event to filter contenttypes data on the store
     *
     * @param {string} value
     * @memberof DotPaletteComponent
     */
    filterContentTypes(value: string): void {
        this.#store.filterContentTypes(value);
    }

    /**
     * Event to paginate contentlets data on the store
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPaletteComponent
     */
    paginateContentlets(event: LazyLoadEvent): void {
        this.#store.getContentletsData(event);
    }

    /**
     * Focus on the contentlet component search field
     *
     * @param {AnimationEvent} event
     * @memberof DotPaletteComponent
     */
    onAnimationDone(event: AnimationEvent): void {
        if (event.toState === 'contentlet:in') {
            this.contentlets.focusInputFilter();
        }

        if (event.toState === 'contentlet:out') {
            this.contentTypes.focusInputFilter();
        }
    }
}
