import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

@Pipe({ name: 'safe' })
export class SafePipe implements PipeTransform {
    constructor(private sanitizer: DomSanitizer) {}

    transform(url) {
        if (url) {
            let urlWithParameters = url;
            urlWithParameters += urlWithParameters.indexOf('?') === -1 ? '?' : '&';
            urlWithParameters +=
                urlWithParameters.indexOf('in_frame') === -1
                    ? 'in_frame=true&frame=detailFrame&container=true'
                    : '';

            return this.sanitizer.bypassSecurityTrustResourceUrl(urlWithParameters);
        } else {
            return this.sanitizer.bypassSecurityTrustResourceUrl('');
        }
    }
}
