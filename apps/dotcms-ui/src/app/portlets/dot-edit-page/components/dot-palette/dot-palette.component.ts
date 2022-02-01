import { animate, state, style, transition, trigger, AnimationEvent } from '@angular/animations';
import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { LazyLoadEvent } from 'primeng/api';
import { Observable } from 'rxjs';
import { DotPaletteContentTypeComponent } from './dot-palette-content-type/dot-palette-content-type.component';
import { DotPaletteContentletsComponent } from './dot-palette-contentlets/dot-palette-contentlets.component';
import { DotPaletteStore } from './store/dot-palette.store';

@Component({
    selector: 'dot-palette',
    templateUrl: './dot-palette.component.html',
    styleUrls: ['./dot-palette.component.scss'],
    providers: [DotPaletteStore],
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
export class DotPaletteComponent implements OnInit {
    @Input() set items(items: DotCMSContentType[]) {
        if (items != null && items.length) {
            this.store.loadContentTypes(items);
        }
    }
    @Input() languageId: string;
    vm$: Observable<any> = this.store.vm$;

    @ViewChild('contentlets') contentlets: DotPaletteContentletsComponent;
    @ViewChild('contentTypes') contentTypes: DotPaletteContentTypeComponent;

    constructor(private store: DotPaletteStore) {}

    ngOnInit(): void {
        this.store.setLanguageId(this.languageId);
    }

    /**
     * Sets value on store to show/hide components on the UI
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        this.store.switchView(variableName);
    }

    /**
     * Event to filter contentlets data on the store
     *
     * @param {string} value
     * @memberof DotPaletteComponent
     */
    filterContentlets(value: string): void {
        this.store.filterContentlets(value);
    }

    /**
     * Event to paginate contentlets data on the store
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPaletteComponent
     */
    paginateContentlets(event: LazyLoadEvent): void {
        this.store.getContentletsData(event);
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
