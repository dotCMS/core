import { Component, Input } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-palette',
    templateUrl: './dot-palette.component.html',
    styleUrls: ['./dot-palette.component.scss']
})
export class DotPaletteComponent {
    @Input() items: DotCMSContentType[] = [];
    @Input() languageId: string;
    contentTypeVariable: string = '';

    constructor() {}

    /**
     * Sets value on contentTypeVariable variable to show/hide components on the UI
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        this.contentTypeVariable = variableName ? variableName : '';
    }
}
