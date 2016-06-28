import { Component, Inject } from "@angular/core";
import { SafeResourceUrl, DomSanitizationService } from '@angular/platform-browser';

import { RouteParams } from '@ngrx/router';
import { Observable } from 'rxjs/Rx'

@Component({
    selector: "iframe-component",
    template: ` 
        <iframe id="detailFrame" name="detailFrame" class="iframe-style" [src]="iframe | async" frameborder="0"></iframe>
    `,
    providers: [],
    styles: [
        `
        .iframe-style {
            width: 100%;
            height: 600px;
        }
        `
    ]
})

export class IframeComponent {
    iframe: Observable<SafeResourceUrl>;
    constructor(params$: RouteParams, @Inject('menuItems') private menuItems:any[], sanitizer: DomSanitizationService) {
        this.iframe = params$.pluck<string>('id')
            .distinctUntilChanged()
            .map(id => {
                return sanitizer.bypassSecurityTrustResourceUrl(menuItems.mapPaths[id]);
            });
    }
}