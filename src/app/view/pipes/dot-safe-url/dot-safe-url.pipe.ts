import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';

@Pipe({ name: 'dotSafeUrl' })
export class DotSafeUrlPipe implements PipeTransform {
    constructor(
        private sanitizer: DomSanitizer,
        private dotRouterService: DotRouterService,
        private activatedRoute: ActivatedRoute
    ) {}

    transform(url) {
        if (url) {
            const urlWithParameters = this.addURLWithParameters(url);
            return this.sanitizer.bypassSecurityTrustResourceUrl(urlWithParameters);
        } else {
            return this.sanitizer.bypassSecurityTrustResourceUrl('');
        }
    }

    private addURLWithParameters(url: string): string {
        let urlWithParameters = url;
        urlWithParameters += url.indexOf('?') === -1 ? '?' : '&';
        urlWithParameters +=
            urlWithParameters.indexOf('in_frame') === -1
                ? `in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=${this
                      .dotRouterService.currentPortlet.id}${this.addQueryParams()}`
                : '';

        return urlWithParameters;
    }

    private addQueryParams(): string {
        let params = '';
        Object.keys(this.activatedRoute.snapshot.queryParams).map(attr => {
            params += `&${attr}=${this.activatedRoute.snapshot.queryParams[attr]}`;
        });
        return params;
    }
}
