import { animate, state, style, transition, trigger, AnimationEvent } from '@angular/animations';
import { Component, Input, ViewChild } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotPaletteContentTypeComponent } from './dot-palette-content-type/dot-palette-content-type.component';
import { DotPaletteContentletsComponent } from './dot-palette-contentlets/dot-palette-contentlets.component';

@Component({
    selector: 'dot-palette',
    templateUrl: './dot-palette.component.html',
    styleUrls: ['./dot-palette.component.scss'],
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
            transition('* => *', animate('200ms ease-in')),
        ])
    ]
})
export class DotPaletteComponent {
    @Input() items: DotCMSContentType[] = [];
    @Input() languageId: string;
    contentTypeVariable = '';
    stateContentlet = 'contentlet:out';

    @ViewChild('contentlets') contentlets: DotPaletteContentletsComponent;
    @ViewChild('contentTypes') contentTypes: DotPaletteContentTypeComponent;

    /**
     * Sets value on contentTypeVariable variable to show/hide components on the UI
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        this.contentTypeVariable = variableName ? variableName : '';
        this.stateContentlet = variableName ? 'contentlet:in' : 'contentlet:out';
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
