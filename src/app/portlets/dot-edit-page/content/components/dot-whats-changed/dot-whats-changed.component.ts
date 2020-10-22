import { Component, Input, OnChanges } from '@angular/core';

@Component({
    selector: 'dot-whats-changed',
    templateUrl: './dot-whats-changed.component.html',
    styleUrls: ['./dot-whats-changed.component.scss']
})
export class DotWhatsChangedComponent implements OnChanges {
    @Input()
    languageId: string;
    @Input()
    pageId: string;
    url: string;

    constructor() {}

    ngOnChanges(): void {
        this.url = `/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=${this.pageId}&pageLang=${this.languageId}`;
    }
}
