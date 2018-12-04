import { Component, OnChanges, Input, SimpleChanges } from '@angular/core';
import { SiteService, Site } from 'dotcms-js';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    styleUrls: ['./dot-api-link.component.scss']
})
export class DotApiLinkComponent implements OnChanges {
    @Input() href: string;

    link$: Observable<string>;

    constructor(private siteService: SiteService) {}

    ngOnChanges(changes: SimpleChanges) {
        this.link$ = this.siteService
            .getCurrentSite()
            .pipe(map((site: Site) => `//${site.hostname}${changes.href.currentValue || ''}`));
    }
}
