import { Component, Inject } from '@angular/core';
import { SafeResourceUrl, DomSanitizationService } from '@angular/platform-browser';

import { RouteParams } from '@ngrx/router';
import { Observable } from 'rxjs/Rx';

@Component({
    providers: [],
    selector: 'dot-iframe',
    template: ` 
        <iframe width="100%" height="100%" id="detailFrame" [src]="iframe | async" frameborder="0"></iframe>
    `,
})

export class IframeLegacyComponent {
    iframe: Observable<SafeResourceUrl>;
    constructor(params$: RouteParams, @Inject('menuItems') private menuItems: Array<any>, sanitizer: DomSanitizationService) {
        this.iframe = params$.pluck<string>('id')
            .distinctUntilChanged()
            .map(id => {
                return sanitizer.bypassSecurityTrustResourceUrl(menuItems.mapPaths[id]);
            });
    }
}
