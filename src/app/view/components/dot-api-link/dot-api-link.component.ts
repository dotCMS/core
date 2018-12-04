import { Component, OnInit, Input } from '@angular/core';
import { SiteService, Site } from 'dotcms-js';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    styleUrls: ['./dot-api-link.component.scss']
})
export class DotApiLinkComponent implements OnInit {
    @Input() href: string;

    link$: Observable<string>;

    constructor(private siteService: SiteService) {}

    ngOnInit() {
        this.link$ = this.siteService
            .getCurrentSite()
            .pipe(map((site: Site) => `//${site.hostname}${this.href || ''}`));
    }
}
