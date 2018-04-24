import { Component, OnInit, Input } from '@angular/core';

@Component({
    selector: 'dot-whats-changed',
    templateUrl: './dot-whats-changed.component.html',
    styleUrls: ['./dot-whats-changed.component.scss']
})
export class DotWhatsChangedComponent implements OnInit {
    @Input() pageId: string;
    url: string;

    constructor() {}

    ngOnInit() {
        this.url = `/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=${this.pageId}&pageLang=1`;
    }
}
