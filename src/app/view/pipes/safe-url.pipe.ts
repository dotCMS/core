import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { DotRouterService } from '../../api/services/dot-router/dot-router.service';

@Pipe({ name: 'safe' })
export class SafePipe implements PipeTransform {
    constructor(private sanitizer: DomSanitizer, private dotRouterService: DotRouterService) {}

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
        urlWithParameters += urlWithParameters.indexOf('in_frame') === -1 ?
            `in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=${this.dotRouterService.currentPortlet.id}` :
            '';

        return urlWithParameters;
    }
}
